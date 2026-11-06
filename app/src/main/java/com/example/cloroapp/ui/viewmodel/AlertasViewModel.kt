package com.example.cloroapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloroapp.data.AccionCorrectiva
import com.example.cloroapp.data.AlertaDetalle
import com.example.cloroapp.data.CloroRepository
import com.example.cloroapp.data.EstadisticaPunto
import com.example.cloroapp.data.PreferenciasRepository
import com.example.cloroapp.data.PuntoResumen
import com.example.cloroapp.domain.Periodo
import com.example.cloroapp.domain.RangosCloro
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

/** Detalle de una alerta: acciones correctivas realizadas y cierre por el supervisor. */
@OptIn(ExperimentalCoroutinesApi::class)
class AlertaDetalleViewModel(private val repo: CloroRepository) : ViewModel() {

    private val alertaId = MutableStateFlow(-1L)
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun seleccionar(id: Long) {
        alertaId.value = id
    }

    val alerta: StateFlow<AlertaDetalle?> = alertaId
        .flatMapLatest { repo.observarAlerta(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val acciones: StateFlow<List<AccionCorrectiva>> = alertaId
        .flatMapLatest { repo.observarAcciones(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun consumirMensaje() {
        _mensaje.value = null
    }

    fun registrarAccion(descripcion: String) {
        viewModelScope.launch {
            _mensaje.value = repo.registrarAccionCorrectiva(alertaId.value, descripcion)
                ?: "Acción correctiva registrada."
        }
    }

    fun cerrarAlerta() {
        viewModelScope.launch {
            _mensaje.value = repo.cerrarAlerta(alertaId.value) ?: "Alerta cerrada correctamente."
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
