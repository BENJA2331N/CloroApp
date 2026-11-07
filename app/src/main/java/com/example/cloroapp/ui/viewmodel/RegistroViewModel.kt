package com.example.cloroapp.ui.viewmodel

import android.graphics.Bitmap
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
import com.example.cloroapp.domain.ValidadorFormularios
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
    val errorValor: String? = null,
    val observacion: String = "",
    val fotoEvidencia: Bitmap? = null,
    val clasificacion: ResultadoClasificacion? = null,
    val mensajeEstado: String = "",
    val rangos: RangosCloro = RangosCloro(),
    val controlesHoy: Int = 0,
    val guardando: Boolean = false,
    val mensaje: String? = null
)

/** Registro manual de controles de cloro en terreno con validación centralizada y evidencia fotográfica nativa. */
class RegistroViewModel(private val repo: CloroRepository, prefs: PreferenciasRepository) : ViewModel() {

    private data class Local(
        val seleccionadoId: Long? = null,
        val valorTexto: String = Formato.decimal(4.0),
        val observacion: String = "",
        val fotoEvidencia: Bitmap? = null,
        val guardando: Boolean = false,
        val mensaje: String? = null
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<RegistroUiState> = combine(
        repo.puntos, prefs.rangos, repo.controlesHoy, local
    ) { puntos, rangos, controles, estadoLocal ->
        val errorCampo = ValidadorFormularios.validarValorPpm(estadoLocal.valorTexto)
        val valor = if (errorCampo == null) Formato.parsear(estadoLocal.valorTexto) else null
        val clasificacion = valor?.let { ClasificadorCloro.clasificar(it, rangos) }
        RegistroUiState(
            puntos = puntos,
            puntoSeleccionado = puntos.firstOrNull { it.puntoId == estadoLocal.seleccionadoId } ?: puntos.firstOrNull(),
            valorTexto = estadoLocal.valorTexto,
            valor = valor,
            errorValor = errorCampo,
            observacion = estadoLocal.observacion,
            fotoEvidencia = estadoLocal.fotoEvidencia,
            clasificacion = clasificacion,
            mensajeEstado = clasificacion?.let { ClasificadorCloro.mensaje(it, rangos) }
                ?: (errorCampo ?: "Ingresa un valor entre 0,00 y 10,00 ppm"),
            rangos = rangos,
            controlesHoy = controles,
            guardando = estadoLocal.guardando,
            mensaje = estadoLocal.mensaje
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistroUiState())

    fun seleccionarPunto(id: Long) {
        local.value = local.value.copy(seleccionadoId = id)
    }

    fun cambiarTexto(texto: String) {
        val filtrado = texto.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }.take(6)
        local.value = local.value.copy(valorTexto = filtrado)
    }

    fun cambiarObservacion(texto: String) {
        local.value = local.value.copy(observacion = texto.take(120))
    }

    fun adjuntarFotoEvidencia(bitmap: Bitmap?) {
        local.value = local.value.copy(
            fotoEvidencia = bitmap,
            mensaje = if (bitmap != null) "Fotografía del fotómetro adjuntada correctamente." else null
        )
    }

    /** Ajuste fino con los botones -0,1 / +0,1 (el valor siempre se mantiene entre 0 y 10 ppm). */
    fun ajustar(delta: Double) {
        val actual = Formato.parsear(local.value.valorTexto) ?: 0.0
        val nuevo = (Math.round((actual + delta) * 100.0) / 100.0).coerceIn(0.0, 10.0)
        local.value = local.value.copy(valorTexto = Formato.decimal(nuevo))
    }

    fun consumirMensaje() {
        local.value = local.value.copy(mensaje = null)
    }

    fun registrar() {
        val estado = uiState.value
        val punto = estado.puntoSeleccionado ?: run {
            local.value = local.value.copy(mensaje = "Selecciona un punto de medición.")
            return
        }
        val errorValidacion = ValidadorFormularios.validarValorPpm(estado.valorTexto)
        val valor = estado.valor
        if (errorValidacion != null || valor == null) {
            local.value = local.value.copy(mensaje = errorValidacion ?: "Ingresa un valor válido entre 0,00 y 10,00 ppm.")
            return
        }
        viewModelScope.launch {
            local.value = local.value.copy(guardando = true)
            val resultadoMensaje = try {
                val nivel = repo.registrarMedicionManual(punto.puntoId, valor)
                val alerta = if (nivel.estado == EstadoCloro.NORMAL) "" else " Revisa la sección Alertas."
                val pendiente = if (nivel.sincronizado) "" else " Pendiente de sincronización."
                val conFoto = if (estado.fotoEvidencia != null) " (con evidencia fotográfica)" else ""
                "Medición registrada$conFoto: ${Formato.ppm(nivel.valorPpm)} (${nivel.estado.etiqueta}).$alerta$pendiente"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: "No se pudo registrar la medición."
            }
            local.value = local.value.copy(
                guardando = false,
                observacion = "",
                fotoEvidencia = null,
                mensaje = resultadoMensaje
            )
        }
    }
}
