package com.example.cloroapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloroapp.data.ContenedorApp
import com.example.cloroapp.data.Granja
import com.example.cloroapp.data.ResultadoSync
import com.example.cloroapp.data.Usuario
import com.example.cloroapp.data.remote.SimuladorCloro
import com.example.cloroapp.domain.RangosCloro
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SesionUiState(
    val cargando: Boolean = true,
    val usuario: Usuario? = null,
    val iniciando: Boolean = false,
    val error: String? = null
)

data class EstadoSync(
    val sincronizando: Boolean = false,
    val mensaje: String? = null
)

/** Sesión del usuario, configuración de la app y actualización periódica de datos. */
class SesionViewModel(contenedor: ContenedorApp) : ViewModel() {

    private val repo = contenedor.repositorio
    private val prefs = contenedor.preferencias

    private val cargandoInicial = MutableStateFlow(true)
    private val iniciando = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val sesion: StateFlow<SesionUiState> = combine(
        repo.usuarioActual, cargandoInicial, iniciando, error
    ) { usuario, cargando, iniciandoSesion, mensajeError ->
        SesionUiState(cargando, usuario, iniciandoSesion, mensajeError)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SesionUiState())

    val rangos: StateFlow<RangosCloro> =
        prefs.rangos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RangosCloro())
    val notificacionesActivas: StateFlow<Boolean> =
        prefs.notificacionesActivas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val simularSinConexion: StateFlow<Boolean> =
        prefs.simularSinConexion.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val ultimaSincronizacion: StateFlow<Long> =
        prefs.ultimaSincronizacion.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val pendientesSincronizacion: StateFlow<Int> =
        repo.pendientesSincronizacion.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val alertasPendientes: StateFlow<Int> =
        repo.alertasPendientes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _estadoSync = MutableStateFlow(EstadoSync())
    val estadoSync: StateFlow<EstadoSync> = _estadoSync.asStateFlow()

    private var jobSync: Job? = null

    init {
        viewModelScope.launch {
            repo.restaurarSesion()
            cargandoInicial.value = false
        }
        // La actualización periódica corre mientras haya un usuario con sesión iniciada.
        viewModelScope.launch {
            repo.usuarioActual.collect { usuario ->
                if (usuario != null) iniciarActualizacionPeriodica() else detenerActualizacionPeriodica()
            }
        }
    }

    fun iniciarSesion(correo: String, contrasena: String) {
        if (correo.isBlank() || contrasena.isBlank()) {
            error.value = "Ingresa tu correo y tu contraseña."
            return
        }
        viewModelScope.launch {
            iniciando.value = true
            error.value = null
            val usuario = repo.iniciarSesion(correo, contrasena)
            iniciando.value = false
            if (usuario == null) error.value = "Correo o contraseña incorrectos."
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            repo.cerrarSesion()
            _estadoSync.value = EstadoSync()
        }
    }

    suspend fun obtenerGranjas(): List<Granja> = repo.granjasDelUsuario()

    // ---------------------------------------------------------------- Sincronización

    fun sincronizarAhora() {
        viewModelScope.launch { ejecutarSincronizacion() }
    }

    private fun iniciarActualizacionPeriodica() {
        jobSync?.cancel()
        jobSync = viewModelScope.launch {
            while (true) {
                ejecutarSincronizacion()
                delay(SimuladorCloro.INTERVALO_MS)
            }
        }
    }

    private fun detenerActualizacionPeriodica() {
        jobSync?.cancel()
        jobSync = null
    }

    private suspend fun ejecutarSincronizacion() {
        _estadoSync.value = EstadoSync(sincronizando = true)
        val mensaje = when (val resultado = repo.sincronizar()) {
            is ResultadoSync.Exito ->
                "Datos actualizados (${resultado.nuevas} mediciones nuevas, ${resultado.enviadas} controles enviados)."
            ResultadoSync.SinConexion -> "Sin conexión: se muestran los datos guardados en el teléfono."
            is ResultadoSync.Error -> "No se pudo actualizar: ${resultado.mensaje}"
        }
        _estadoSync.value = EstadoSync(sincronizando = false, mensaje = mensaje)
    }

    // ---------------------------------------------------------------- Configuración

    fun cambiarNotificaciones(activadas: Boolean) {
        viewModelScope.launch { prefs.guardarNotificaciones(activadas) }
    }

    fun cambiarSimularSinConexion(activado: Boolean) {
        viewModelScope.launch { prefs.guardarSimularSinConexion(activado) }
    }

    /** Devuelve false si los rangos no son coherentes (deben ir de menor a mayor). */
    fun guardarRangos(rangos: RangosCloro): Boolean {
        if (!rangos.esValido()) return false
        viewModelScope.launch { prefs.guardarRangos(rangos) }
        return true
    }

    fun restablecerRangos() {
        viewModelScope.launch { prefs.restablecerRangos() }
    }
}
