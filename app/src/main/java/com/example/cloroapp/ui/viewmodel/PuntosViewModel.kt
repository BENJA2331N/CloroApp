package com.example.cloroapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloroapp.data.CloroRepository
import com.example.cloroapp.data.NivelCloro
import com.example.cloroapp.data.PreferenciasRepository
import com.example.cloroapp.data.PuntoResumen
import com.example.cloroapp.domain.Periodo
import com.example.cloroapp.domain.RangosCloro
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Lista de granjas y puntos con su estado actual. */
class PuntosViewModel(repo: CloroRepository) : ViewModel() {
    val puntos: StateFlow<List<PuntoResumen>> =
        repo.puntos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Detalle de un punto: estado actual e historial por período (24 h, 7 d, 30 d). */
@OptIn(ExperimentalCoroutinesApi::class)
class PuntoDetalleViewModel(repo: CloroRepository, prefs: PreferenciasRepository) : ViewModel() {

    private val puntoId = MutableStateFlow(-1L)
    private val _periodo = MutableStateFlow(Periodo.H24)
    val periodo: StateFlow<Periodo> = _periodo.asStateFlow()

    fun seleccionarPunto(id: Long) {
        puntoId.value = id
    }

    fun cambiarPeriodo(nuevo: Periodo) {
        _periodo.value = nuevo
    }

    val punto: StateFlow<PuntoResumen?> = puntoId
        .flatMapLatest { repo.observarPunto(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val historial: StateFlow<List<NivelCloro>> = combine(puntoId, _periodo) { id, periodo -> id to periodo }
        .flatMapLatest { (id, periodo) -> repo.observarHistorial(id, periodo.desdeMillis()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rangos: StateFlow<RangosCloro> =
        prefs.rangos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RangosCloro())
}
