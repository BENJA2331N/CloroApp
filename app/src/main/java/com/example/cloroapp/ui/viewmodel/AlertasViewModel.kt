package com.example.cloroapp.ui.viewmodel

import android.graphics.Bitmap
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

/** Detalle de una alerta: acciones correctivas con foto en SQLite, estado actual del punto y cierre. */
@OptIn(ExperimentalCoroutinesApi::class)
class AlertaDetalleViewModel(private val repo: CloroRepository) : ViewModel() {

    private val alertaId = MutableStateFlow(-1L)
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _descripcionAccion = MutableStateFlow("")
    val descripcionAccion: StateFlow<String> = _descripcionAccion.asStateFlow()

    private val _errorDescripcion = MutableStateFlow<String?>(null)
    val errorDescripcion: StateFlow<String?> = _errorDescripcion.asStateFlow()

    private val _fotoEvidencia = MutableStateFlow<Bitmap?>(null)
    val fotoEvidencia: StateFlow<Bitmap?> = _fotoEvidencia.asStateFlow()

    fun seleccionar(id: Long) {
        alertaId.value = id
    }

    val alerta: StateFlow<AlertaDetalle?> = alertaId
        .flatMapLatest { repo.observarAlerta(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Observa en vivo la última medición del punto afectado (para ver la medición de verificación). */
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

    fun adjuntarFotoEvidencia(bitmap: Bitmap?) {
        _fotoEvidencia.value = bitmap
        if (bitmap != null) {
            _mensaje.value = "Fotografía de respaldo capturada y lista para guardarse."
        }
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
            val fotoBase64 = _fotoEvidencia.value?.let { bmp ->
                withContext(Dispatchers.Default) { ImagenUtils.bitmapABase64(bmp) }
            }
            val respuesta = repo.registrarAccionCorrectiva(
                alertaId = alertaId.value,
                descripcion = descripcion.trim(),
                fotoBase64 = fotoBase64
            )
            if (respuesta == null) {
                _descripcionAccion.value = ""
                _errorDescripcion.value = null
                _fotoEvidencia.value = null
                _mensaje.value = if (fotoBase64 != null) {
                    "Acción correctiva y fotografía guardadas en la base de datos."
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
