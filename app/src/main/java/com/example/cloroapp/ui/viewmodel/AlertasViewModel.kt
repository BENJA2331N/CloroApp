package com.example.cloroapp.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloroapp.data.AccionCorrectiva
import com.example.cloroapp.data.AlertaDetalle
import com.example.cloroapp.data.CloroRepository
import com.example.cloroapp.data.EstadisticaPunto
import com.example.cloroapp.data.ImagenUtils
import com.example.cloroapp.data.PreferenciasRepository
import com.example.cloroapp.data.PuntoResumen
import com.example.cloroapp.domain.Periodo
import com.example.cloroapp.domain.RangosCloro
import com.example.cloroapp.domain.ValidadorFormularios
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Lista de alertas pendientes o historial completo (respuesta 24). */
@OptIn(ExperimentalCoroutinesApi::class)
class AlertasViewModel(repo: CloroRepository) : ViewModel() {

    private val _soloPendientes = MutableStateFlow(true)
    val soloPendientes: StateFlow<Boolean> = _soloPendientes.asStateFlow()

    fun cambiarFiltro(soloPendientes: Boolean) {
        _soloPendientes.value = soloPendientes
    }

    val alertas: StateFlow<List<AlertaDetalle>> = _soloPendientes
        .flatMapLatest { repo.observarAlertas(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Detalle de una alerta: acciones correctivas con foto en archivo persistente y Room. */
@OptIn(ExperimentalCoroutinesApi::class)
class AlertaDetalleViewModel(private val repo: CloroRepository) : ViewModel() {

    private val alertaId = MutableStateFlow(-1L)
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _descripcionAccion = MutableStateFlow("")
    val descripcionAccion: StateFlow<String> = _descripcionAccion.asStateFlow()

    private val _errorDescripcion = MutableStateFlow<String?>(null)
    val errorDescripcion: StateFlow<String?> = _errorDescripcion.asStateFlow()

    private val _fotoUri = MutableStateFlow<Uri?>(null)
    val fotoUri: StateFlow<Uri?> = _fotoUri.asStateFlow()

    private val _fotoRuta = MutableStateFlow<String?>(null)
    val fotoRuta: StateFlow<String?> = _fotoRuta.asStateFlow()

    private val _fotoBitmap = MutableStateFlow<Bitmap?>(null)
    val fotoBitmap: StateFlow<Bitmap?> = _fotoBitmap.asStateFlow()

    fun seleccionar(id: Long) {
        alertaId.value = id
    }

    val alerta: StateFlow<AlertaDetalle?> = alertaId
        .flatMapLatest { repo.observarAlerta(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val puntoActual: StateFlow<PuntoResumen?> = alerta
        .flatMapLatest { det ->
            if (det == null) flowOf(null)
            else repo.observarPunto(det.alerta.puntoMedicionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val acciones: StateFlow<List<AccionCorrectiva>> = alertaId
        .flatMapLatest { repo.observarAcciones(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun actualizarDescripcion(texto: String) {
        val recortado = texto.take(ValidadorFormularios.MAX_CARACTERES_ACCION)
        _descripcionAccion.value = recortado
        _errorDescripcion.value = if (recortado.isEmpty()) null else ValidadorFormularios.validarAccionCorrectiva(recortado)
    }

    fun prepararCapturaFoto(context: Context): Uri {
        val (uri, archivo) = ImagenUtils.crearUriFotoTemporal(context)
        _fotoUri.value = uri
        _fotoRuta.value = archivo.absolutePath
        return uri
    }

    fun confirmarFotoCapturada(context: Context) {
        val uri = _fotoUri.value ?: return
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                ImagenUtils.cargarBitmapDesdeUri(context, uri)
            }
            _fotoBitmap.value = bitmap
            _mensaje.value = "Foto guardada exitosamente"
        }
    }

    fun eliminarFoto() {
        _fotoRuta.value?.let { ruta ->
            try { File(ruta).delete() } catch (_: Exception) {}
        }
        _fotoUri.value = null
        _fotoRuta.value = null
        _fotoBitmap.value = null
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }

    fun registrarAccion(descripcion: String = _descripcionAccion.value) {
        val error = ValidadorFormularios.validarAccionCorrectiva(descripcion)
        if (error != null) {
            _errorDescripcion.value = error
            _mensaje.value = error
            return
        }
        viewModelScope.launch {
            val ruta = _fotoRuta.value
            val respuesta = repo.registrarAccionCorrectiva(
                alertaId = alertaId.value,
                descripcion = descripcion.trim(),
                fotoRuta = ruta
            )
            if (respuesta == null) {
                _descripcionAccion.value = ""
                _errorDescripcion.value = null
                _fotoUri.value = null
                _fotoRuta.value = null
                _fotoBitmap.value = null
                _mensaje.value = if (!ruta.isNullOrBlank()) {
                    "Acción correctiva registrada con foto guardada exitosamente"
                } else {
                    "Acción correctiva registrada correctamente."
                }
            } else {
                _errorDescripcion.value = respuesta
                _mensaje.value = respuesta
            }
        }
    }

    fun cerrarAlerta() {
        viewModelScope.launch {
            _mensaje.value = repo.cerrarAlerta(alertaId.value) ?: "Alerta validada y cerrada correctamente."
        }
    }
}

/** Panel de indicadores para supervisores y jefaturas (respuesta 28). */
@OptIn(ExperimentalCoroutinesApi::class)
class PanelViewModel(repo: CloroRepository, prefs: PreferenciasRepository) : ViewModel() {

    private val _periodo = MutableStateFlow(Periodo.D7)
    val periodo: StateFlow<Periodo> = _periodo.asStateFlow()

    private val _granjaId = MutableStateFlow<Long?>(null)
    val granjaId: StateFlow<Long?> = _granjaId.asStateFlow()

    fun cambiarPeriodo(nuevo: Periodo) {
        _periodo.value = nuevo
    }

    fun cambiarGranja(id: Long?) {
        _granjaId.value = id
    }

    val estadisticas: StateFlow<List<EstadisticaPunto>> = _periodo
        .flatMapLatest { repo.observarEstadisticas(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val puntosActuales: StateFlow<List<PuntoResumen>> =
        repo.puntos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val alertasPendientes: StateFlow<Int> =
        repo.alertasPendientes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val rangos: StateFlow<RangosCloro> =
        prefs.rangos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RangosCloro())
}
