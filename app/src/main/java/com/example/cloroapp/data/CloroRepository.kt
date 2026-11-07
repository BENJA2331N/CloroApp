package com.example.cloroapp.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.withTransaction
import com.example.cloroapp.data.remote.ApiConfig
import com.example.cloroapp.data.remote.CloroApiService
import com.example.cloroapp.data.remote.MedicionDto
import com.example.cloroapp.data.remote.SimuladorCloro
import com.example.cloroapp.domain.ClasificadorCloro
import com.example.cloroapp.domain.EstadoAlerta
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.EstadoResolucion
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.domain.OrigenMedicion
import com.example.cloroapp.domain.Periodo
import com.example.cloroapp.domain.RangosCloro
import com.example.cloroapp.domain.ResultadoClasificacion
import com.example.cloroapp.domain.Rol
import com.example.cloroapp.domain.Seguridad
import com.example.cloroapp.domain.TipoPunto
import com.example.cloroapp.notificaciones.Notificador
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

sealed interface ResultadoSync {
    data class Exito(val nuevas: Int, val enviadas: Int) : ResultadoSync
    data object SinConexion : ResultadoSync
    data class Error(val mensaje: String) : ResultadoSync
}

/**
 * Punto único de acceso a datos: combina Room (almacenamiento local), la API (Retrofit) y las
 * reglas de negocio del caso: clasificación por rangos, generación de alertas, acciones correctivas
 * y cierre validado por un supervisor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CloroRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val api: CloroApiService,
    private val prefs: PreferenciasRepository,
    private val notificador: Notificador
) {
    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    private val mutexDatos = Mutex()
    private val mutexSync = Mutex()
    private var datosListos = false

    // ---------------------------------------------------------------- Sesión

    suspend fun iniciarSesion(correo: String, contrasena: String): Usuario? {
        asegurarDatosIniciales()
        val usuario = db.usuarioDao().obtenerPorCorreo(correo.trim().lowercase()) ?: return null
        if (usuario.contrasenaHash != Seguridad.hash(usuario.correo, contrasena)) return null
        _usuarioActual.value = usuario
        prefs.guardarSesion(usuario.id)
        return usuario
    }

    suspend fun restaurarSesion() {
        val id = prefs.sesionUsuarioId.first() ?: return
        _usuarioActual.value = db.usuarioDao().obtenerPorId(id)
    }

    suspend fun cerrarSesion() {
        _usuarioActual.value = null
        prefs.cerrarSesion()
    }

    suspend fun granjasDelUsuario(): List<Granja> {
        val usuario = _usuarioActual.value ?: return emptyList()
        return if (usuario.rol == Rol.JEFATURA) {
            db.granjaDao().obtenerTodas()
        } else {
            db.usuarioDao().obtenerGranjasAsignadas(usuario.id)
        }
    }

    // ---------------------------------------------------------------- Consultas observables

    /** Puntos con su última medición: todos para la jefatura, solo los asignados para los demás. */
    val puntos: Flow<List<PuntoResumen>> = _usuarioActual.flatMapLatest { u ->
        if (u == null) flowOf(emptyList())
        else db.puntoMedicionDao().observarResumen(u.rol == Rol.JEFATURA, u.id)
    }

    fun observarPunto(puntoId: Long): Flow<PuntoResumen?> =
        db.puntoMedicionDao().observarResumenPunto(puntoId)

    fun observarHistorial(puntoId: Long, desdeMillis: Long): Flow<List<NivelCloro>> =
        db.nivelCloroDao().observarHistorial(puntoId, desdeMillis)

    fun observarAlertas(soloPendientes: Boolean): Flow<List<AlertaDetalle>> =
        _usuarioActual.flatMapLatest { u ->
            if (u == null) flowOf(emptyList())
            else db.alertaDao().observarAlertas(soloPendientes, u.rol == Rol.JEFATURA, u.id)
        }

    fun observarAlerta(alertaId: Long): Flow<AlertaDetalle?> = db.alertaDao().observarAlerta(alertaId)

    fun observarAcciones(alertaId: Long): Flow<List<AccionCorrectiva>> =
        db.accionCorrectivaDao().observarPorAlerta(alertaId)

    val alertasPendientes: Flow<Int> = _usuarioActual.flatMapLatest { u ->
        if (u == null) flowOf(0)
        else db.alertaDao().observarCantidadPendientes(u.rol == Rol.JEFATURA, u.id)
    }

    fun observarEstadisticas(periodo: Periodo): Flow<List<EstadisticaPunto>> =
        _usuarioActual.flatMapLatest { u ->
            if (u == null) flowOf(emptyList())
            else db.nivelCloroDao().observarEstadisticas(periodo.desdeMillis(), u.rol == Rol.JEFATURA, u.id)
        }

    val pendientesSincronizacion: Flow<Int> = db.nivelCloroDao().observarCantidadPendientes()

    /** Controles manuales registrados hoy por el usuario con sesión activa. */
    val controlesHoy: Flow<Int> = _usuarioActual.flatMapLatest { u ->
        if (u == null) flowOf(0)
        else db.nivelCloroDao().observarControlesManuales(u.nombre, inicioDelDia())
    }

    // ---------------------------------------------------------------- Registro de mediciones

    /**
     * Registra un control manual junto con su observación y fotografía en Base64 dentro de Room (SQLite).
     * Queda guardado localmente aunque no haya conexión y se envía a la API cuando es posible (respuesta 12).
     * Si la medición está fuera de rango se genera una alerta.
     */
    suspend fun registrarMedicionManual(
        puntoId: Long,
        valor: Double,
        observacion: String? = null,
        fotoBase64: String? = null
    ): NivelCloro {
        val usuario = _usuarioActual.value ?: throw IllegalStateException("No hay una sesión iniciada.")
        require(usuario.rol.puedeRegistrarMediciones) { "Tu perfil no permite registrar mediciones." }
        require(valor in 0.0..10.0) { "El valor debe estar entre 0,00 y 10,00 ppm." }

        val punto = db.puntoMedicionDao().obtenerPorId(puntoId)
            ?: throw IllegalArgumentException("El punto de medición no existe.")
        require(
            usuario.rol == Rol.JEFATURA ||
                db.usuarioDao().contarAsignacion(usuario.id, punto.granjaId) > 0
        ) { "No tienes asignada la granja de este punto." }

        val valorRedondeado = ClasificadorCloro.redondear(valor)
        val clasificacion = ClasificadorCloro.clasificar(valorRedondeado, prefs.rangos.first())
        val nivel = NivelCloro(
            puntoMedicionId = punto.id,
            valorPpm = valorRedondeado,
            fechaHoraMillis = System.currentTimeMillis(),
            estado = clasificacion.estado,
            registradoPor = usuario.nombre,
            origen = OrigenMedicion.MANUAL,
            sincronizado = false,
            observacion = observacion?.trim()?.takeIf { it.isNotEmpty() },
            fotoBase64 = fotoBase64
        )
        val id = db.nivelCloroDao().insertar(nivel)
        if (id == -1L) throw IllegalStateException("Ya existe una medición registrada en ese instante. Inténtalo nuevamente.")

        if (clasificacion.estado == EstadoCloro.CRITICO) {
            notificador.vibrarAlertaCritica()
        } else {
            notificador.vibrarConfirmacion()
        }

        evaluarAlerta(punto, nivel.copy(id = id), clasificacion)
        enviarPendientesSiHayConexion()
        return db.nivelCloroDao().obtenerPorId(id) ?: nivel.copy(id = id)
    }

    // ---------------------------------------------------------------- Alertas y acciones correctivas

    /**
     * Crea la alerta de un punto cuando su medición sale de rango. Hay una sola alerta pendiente por punto:
     * si ya existe y la nueva medición es crítica, se eleva su gravedad. La notificación solo se emite
     * para alertas críticas nuevas o que escalan (respuesta 26).
     */
    private suspend fun evaluarAlerta(punto: PuntoMedicion, nivel: NivelCloro, clasificacion: ResultadoClasificacion) {
        val tipo = clasificacion.desviacion ?: return
        val dao = db.alertaDao()
        val existente = dao.obtenerPendienteDelPunto(punto.id)

        val alertaAfectada: Alerta? = when {
            existente == null -> {
                val nueva = Alerta(
                    puntoMedicionId = punto.id,
                    nivelCloroId = nivel.id,
                    tipo = tipo,
                    severidad = clasificacion.estado,
                    valorPpm = nivel.valorPpm,
                    fechaHoraMillis = nivel.fechaHoraMillis
                )
                nueva.copy(id = dao.insertar(nueva))
            }

            clasificacion.estado == EstadoCloro.CRITICO && existente.severidad != EstadoCloro.CRITICO -> {
                val elevada = existente.copy(
                    nivelCloroId = nivel.id,
                    tipo = tipo,
                    severidad = EstadoCloro.CRITICO,
                    valorPpm = nivel.valorPpm
                )
                dao.actualizar(elevada)
                elevada
            }

            else -> null
        }

        if (alertaAfectada != null && alertaAfectada.severidad == EstadoCloro.CRITICO) {
            notificarSiCorresponde(punto, alertaAfectada)
        }
    }

    /** Solo se avisa a operarios y supervisores responsables de la granja afectada (respuesta 20). */
    private suspend fun notificarSiCorresponde(punto: PuntoMedicion, alerta: Alerta) {
        val usuario = _usuarioActual.value ?: return
        if (usuario.rol == Rol.JEFATURA) return
        if (!prefs.notificacionesActivas.first()) return
        if (db.usuarioDao().contarAsignacion(usuario.id, punto.granjaId) == 0) return

        val granja = db.granjaDao().obtenerPorId(punto.granjaId)
        notificador.mostrarAlertaCritica(
            alertaId = alerta.id,
            titulo = "Alerta crítica · ${punto.codigo}",
            texto = "${alerta.tipo.etiqueta}: ${Formato.ppm(alerta.valorPpm)} en " +
                "${granja?.nombre ?: "la granja"}, ${punto.galpon}."
        )
    }

    /** Devuelve un mensaje de error, o null si la acción y su fotografía quedaron registradas en SQLite. */
    suspend fun registrarAccionCorrectiva(
        alertaId: Long,
        descripcion: String,
        fotoBase64: String? = null
    ): String? {
        val usuario = _usuarioActual.value ?: return "No hay una sesión iniciada."
        if (!usuario.rol.puedeRegistrarMediciones) return "Tu perfil no permite registrar acciones correctivas."
        val errorValidacion = com.example.cloroapp.domain.ValidadorFormularios.validarAccionCorrectiva(descripcion)
        if (errorValidacion != null) return errorValidacion
        val texto = descripcion.trim()

        val alerta = db.alertaDao().obtenerPorId(alertaId) ?: return "La alerta no existe."
        if (alerta.estado == EstadoAlerta.CERRADA) return "La alerta ya está cerrada."

        db.accionCorrectivaDao().insertar(
            AccionCorrectiva(
                alertaId = alerta.id,
                puntoMedicionId = alerta.puntoMedicionId,
                nivelCloroId = alerta.nivelCloroId,
                descripcion = texto,
                responsable = usuario.nombre,
                fechaHoraMillis = System.currentTimeMillis(),
                fotoBase64 = fotoBase64
            )
        )
        if (alerta.estado == EstadoAlerta.ABIERTA) {
            db.alertaDao().actualizar(alerta.copy(estado = EstadoAlerta.EN_GESTION))
        }
        return null
    }

    /**
     * El supervisor cierra la alerta una vez registrada la acción correctiva y verificada una nueva
     * medición dentro del rango aceptable (respuestas 21 y 22). Devuelve un mensaje de error o null.
     */
    suspend fun cerrarAlerta(alertaId: Long): String? {
        val usuario = _usuarioActual.value ?: return "No hay una sesión iniciada."
        if (!usuario.rol.puedeCerrarAlertas) return "Solo un supervisor puede cerrar alertas."

        val alerta = db.alertaDao().obtenerPorId(alertaId) ?: return "La alerta no existe."
        if (alerta.estado == EstadoAlerta.CERRADA) return "La alerta ya está cerrada."

        val ultimaAccion = db.accionCorrectivaDao().obtenerFechaUltimaAccion(alertaId)
            ?: return "Registra al menos una acción correctiva antes de cerrar la alerta."

        val ultima = db.nivelCloroDao().obtenerUltima(alerta.puntoMedicionId)
        if (ultima == null || ultima.fechaHoraMillis <= ultimaAccion) {
            return "Registra una nueva medición del punto después de la acción correctiva."
        }
        if (ultima.estado != EstadoCloro.NORMAL) {
            return "La última medición (${Formato.ppm(ultima.valorPpm)}) sigue fuera del rango normal."
        }

        db.accionCorrectivaDao().validarTodas(alertaId)
        db.alertaDao().actualizar(
            alerta.copy(
                estado = EstadoAlerta.CERRADA,
                cerradaPor = usuario.nombre,
                fechaCierreMillis = System.currentTimeMillis()
            )
        )
        return null
    }

    // ---------------------------------------------------------------- Sincronización con la API

    /**
     * Envía los controles pendientes y descarga las últimas mediciones. Sin conexión no hace nada:
     * la app sigue mostrando los datos guardados localmente (respuesta 12).
     */
    suspend fun sincronizar(): ResultadoSync = mutexSync.withLock {
        if (!hayConexion()) return@withLock ResultadoSync.SinConexion
        try {
            val enviadas = enviarPendientes()
            sincronizarCatalogo()

            val rangos = prefs.rangos.first()
            var nuevas = 0
            for (dto in api.obtenerUltimasMediciones()) {
                if (procesarMedicionRemota(dto, rangos)) nuevas++
            }
            prefs.guardarUltimaSincronizacion(System.currentTimeMillis())
            ResultadoSync.Exito(nuevas, enviadas)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoSync.Error(e.message ?: "Error de comunicación con el servidor")
        }
    }

    private suspend fun procesarMedicionRemota(dto: MedicionDto, rangos: RangosCloro): Boolean {
        val punto = db.puntoMedicionDao().obtenerPorCodigo(dto.codigoPunto) ?: return false
        val valor = ClasificadorCloro.redondear(dto.valorPpm)
        val clasificacion = ClasificadorCloro.clasificar(valor, rangos)

        val id = db.nivelCloroDao().insertar(
            NivelCloro(
                puntoMedicionId = punto.id,
                valorPpm = valor,
                fechaHoraMillis = dto.fechaHoraMillis,
                estado = clasificacion.estado,
                registradoPor = "Sistema de monitoreo",
                origen = OrigenMedicion.API,
                sincronizado = true
            )
        )

        // Solo se evalúa la alerta si esta es la medición más reciente del punto (no una anterior a un control manual).
        val ultima = db.nivelCloroDao().obtenerUltima(punto.id)
        if (ultima != null && ultima.fechaHoraMillis == dto.fechaHoraMillis) {
            evaluarAlerta(punto, ultima, ClasificadorCloro.clasificar(ultima.valorPpm, rangos))
        }
        return id != -1L
    }

    private suspend fun sincronizarCatalogo() {
        val granjaDao = db.granjaDao()
        val puntoDao = db.puntoMedicionDao()
        for (dto in api.obtenerPuntos()) {
            var granja = granjaDao.obtenerPorNombre(dto.granja)
            if (granja == null) {
                val id = granjaDao.insertar(Granja(nombre = dto.granja, ubicacion = dto.ubicacionGranja))
                granja = granjaDao.obtenerPorId(id) ?: continue
            }
            if (puntoDao.obtenerPorCodigo(dto.codigo) == null) {
                puntoDao.insertar(
                    PuntoMedicion(
                        granjaId = granja.id,
                        galpon = dto.galpon,
                        codigo = dto.codigo,
                        ubicacion = dto.ubicacion,
                        tipo = runCatching { TipoPunto.valueOf(dto.tipo) }.getOrDefault(TipoPunto.BEBEDERO)
                    )
                )
            }
        }
    }

    private suspend fun enviarPendientes(): Int {
        var enviadas = 0
        for (nivel in db.nivelCloroDao().obtenerPendientes()) {
            val punto = db.puntoMedicionDao().obtenerPorId(nivel.puntoMedicionId) ?: continue
            api.enviarMedicion(MedicionDto(punto.codigo, nivel.valorPpm, nivel.fechaHoraMillis))
            db.nivelCloroDao().marcarSincronizada(nivel.id)
            enviadas++
        }
        return enviadas
    }

    private suspend fun enviarPendientesSiHayConexion() {
        mutexSync.withLock {
            if (!hayConexion()) return@withLock
            try {
                enviarPendientes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Queda pendiente de sincronización y se reintentará en la próxima actualización.
            }
        }
    }

    private suspend fun hayConexion(): Boolean = when {
        prefs.simularSinConexion.first() -> false
        ApiConfig.USAR_API_SIMULADA -> true
        else -> redDisponible()
    }

    private fun redDisponible(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val red = manager.activeNetwork ?: return false
        val capacidades = manager.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ---------------------------------------------------------------- Datos iniciales ficticios

    /** Carga usuarios, granjas, puntos e historial ficticios la primera vez que se abre la app. */
    suspend fun asegurarDatosIniciales() = mutexDatos.withLock {
        if (datosListos) return@withLock
        db.withTransaction {
            if (db.usuarioDao().contar() == 0) cargarUsuariosGranjasYPuntos()
            if (db.nivelCloroDao().contar() == 0) cargarHistorial()
        }
        datosListos = true
    }

    private suspend fun cargarUsuariosGranjasYPuntos() {
        val granjaIds = mutableListOf<Long>()
        for (g in SimuladorCloro.puntos.distinctBy { it.granja }) {
            granjaIds += db.granjaDao().insertar(Granja(nombre = g.granja, ubicacion = g.ubicacionGranja))
        }
        for (p in SimuladorCloro.puntos) {
            val granja = db.granjaDao().obtenerPorNombre(p.granja) ?: continue
            db.puntoMedicionDao().insertar(
                PuntoMedicion(granjaId = granja.id, galpon = p.galpon, codigo = p.codigo, ubicacion = p.ubicacion, tipo = p.tipo)
            )
        }
        for (u in DatosIniciales.usuarios) {
            val usuarioId = db.usuarioDao().insertar(
                Usuario(
                    nombre = u.nombre,
                    correo = u.correo,
                    contrasenaHash = Seguridad.hash(u.correo, DatosIniciales.CLAVE_DEMO),
                    rol = u.rol
                )
            )
            for (indice in u.granjas) {
                granjaIds.getOrNull(indice)?.let { db.usuarioDao().asignarGranja(UsuarioGranja(usuarioId, it)) }
            }
        }
    }

    private suspend fun cargarHistorial() {
        val rangos = RangosCloro()
        val idPorCodigo = db.puntoMedicionDao().let { dao ->
            SimuladorCloro.puntos.mapNotNull { p -> dao.obtenerPorCodigo(p.codigo)?.let { p.codigo to it.id } }.toMap()
        }
        val mediciones = SimuladorCloro.historial(dias = 30, ahoraMs = System.currentTimeMillis())
            .mapNotNull { dto ->
                val puntoId = idPorCodigo[dto.codigoPunto] ?: return@mapNotNull null
                NivelCloro(
                    puntoMedicionId = puntoId,
                    valorPpm = dto.valorPpm,
                    fechaHoraMillis = dto.fechaHoraMillis,
                    estado = ClasificadorCloro.clasificar(dto.valorPpm, rangos).estado,
                    registradoPor = "Sistema de monitoreo",
                    origen = OrigenMedicion.API,
                    sincronizado = true
                )
            }
        db.nivelCloroDao().insertarTodas(mediciones)
    }

    private fun inicioDelDia(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
