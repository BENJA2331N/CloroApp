package com.example.cloroapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloroapp.data.CloroRepository
import com.example.cloroapp.data.PreferenciasRepository
import com.example.cloroapp.data.PuntoResumen
import com.example.cloroapp.domain.ClasificadorCloro
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.domain.RangosCloro
import com.example.cloroapp.domain.ResultadoClasificacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RegistroUiState(
    val puntos: List<PuntoResumen> = emptyList(),
    val puntoSeleccionado: PuntoResumen? = null,
    val valorTexto: String = "4,00",
    val valor: Double? = 4.0,
    val clasificacion: ResultadoClasificacion? = null,
    val mensajeEstado: String = "",
    val rangos: RangosCloro = RangosCloro(),
    val controlesHoy: Int = 0,
    val guardando: Boolean = false,
    val mensaje: String? = null
)

/** Registro manual de controles de cloro en terreno (operarios y supervisores). */
class RegistroViewModel(private val repo: CloroRepository, prefs: PreferenciasRepository) : ViewModel() {

    private data class Local(
        val seleccionadoId: Long?,
        val valorTexto: String,
        val guardando: Boolean,
        val mensaje: String?
    )

    private val seleccionadoId = MutableStateFlow<Long?>(null)
    private val valorTexto = MutableStateFlow(Formato.decimal(4.0))
    private val guardando = MutableStateFlow(false)
    private val mensaje = MutableStateFlow<String?>(null)

    private val local = combine(seleccionadoId, valorTexto, guardando, mensaje) { id, texto, ocupado, msg ->
        Local(id, texto, ocupado, msg)
    }

    val uiState: StateFlow<RegistroUiState> = combine(
        repo.puntos, prefs.rangos, repo.controlesHoy, local
    ) { puntos, rangos, controles, estadoLocal ->
        val valor = Formato.parsear(estadoLocal.valorTexto)?.takeIf { it in 0.0..10.0 }
        val clasificacion = valor?.let { ClasificadorCloro.clasificar(it, rangos) }
        RegistroUiState(
            puntos = puntos,
            puntoSeleccionado = puntos.firstOrNull { it.puntoId == estadoLocal.seleccionadoId } ?: puntos.firstOrNull(),
            valorTexto = estadoLocal.valorTexto,
            valor = valor,
            clasificacion = clasificacion,
            mensajeEstado = clasificacion?.let { ClasificadorCloro.mensaje(it, rangos) }
                ?: "Ingresa un valor entre 0,00 y 10,00 ppm",
            rangos = rangos,
            controlesHoy = controles,
            guardando = estadoLocal.guardando,
            mensaje = estadoLocal.mensaje
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistroUiState())

    fun seleccionarPunto(id: Long) {
        seleccionadoId.value = id
    }

    fun cambiarTexto(texto: String) {
        valorTexto.value = texto.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
    }

    /** Ajuste fino con los botones -0,1 / +0,1 (el valor siempre se mantiene entre 0 y 10 ppm). */
    fun ajustar(delta: Double) {
        val actual = Formato.parsear(valorTexto.value) ?: 0.0
        val nuevo = (Math.round((actual + delta) * 100.0) / 100.0).coerceIn(0.0, 10.0)
        valorTexto.value = Formato.decimal(nuevo)
    }

    fun consumirMensaje() {
        mensaje.value = null
    }

    fun registrar() {
        val estado = uiState.value
        val punto = estado.puntoSeleccionado ?: run {
            mensaje.value = "Selecciona un punto de medición."
            return
        }
        val valor = estado.valor ?: run {
            mensaje.value = "Ingresa un valor válido entre 0,00 y 10,00 ppm."
            return
        }
        viewModelScope.launch {
            guardando.value = true
            mensaje.value = try {
                val nivel = repo.registrarMedicionManual(punto.puntoId, valor)
                val alerta = if (nivel.estado == EstadoCloro.NORMAL) "" else " Revisa la sección Alertas."
                val pendiente = if (nivel.sincronizado) "" else " Pendiente de sincronización."
                "Medición registrada: ${Formato.ppm(nivel.valorPpm)} (${nivel.estado.etiqueta}).$alerta$pendiente"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: "No se pudo registrar la medición."
            }
            guardando.value = false
        }
    }
}
