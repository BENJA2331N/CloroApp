package com.example.cloroapp.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloroapp.data.CloroRepository
import com.example.cloroapp.data.ImagenUtils
import com.example.cloroapp.data.PreferenciasRepository
import com.example.cloroapp.data.PuntoResumen
import com.example.cloroapp.domain.ClasificadorCloro
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.domain.RangosCloro
import com.example.cloroapp.domain.ResultadoClasificacion
import com.example.cloroapp.domain.ValidadorFormularios
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class RegistroUiState(
    val puntos: List<PuntoResumen> = emptyList(),
    val puntoSeleccionado: PuntoResumen? = null,
    val valorTexto: String = "4,00",
    val valor: Double? = 4.0,
    val errorValor: String? = null,
    val observacion: String = "",
    val fotoUri: Uri? = null,
    val fotoRuta: String? = null,
    val fotoBitmap: Bitmap? = null,
    val clasificacion: ResultadoClasificacion? = null,
    val mensajeEstado: String = "",
    val rangos: RangosCloro = RangosCloro(),
    val controlesHoy: Int = 0,
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val registroExitoso: Boolean = false
)

/** Registro manual de controles de cloro en terreno con persistencia segura de foto mediante FileProvider y Room. */
class RegistroViewModel(private val repo: CloroRepository, prefs: PreferenciasRepository) : ViewModel() {

    private data class Local(
        val seleccionadoId: Long? = null,
        val valorTexto: String = Formato.decimal(4.0),
        val observacion: String = "",
        val fotoUri: Uri? = null,
        val fotoRuta: String? = null,
        val fotoBitmap: Bitmap? = null,
        val guardando: Boolean = false,
        val mensaje: String? = null,
        val registroExitoso: Boolean = false
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
            fotoUri = estadoLocal.fotoUri,
            fotoRuta = estadoLocal.fotoRuta,
            fotoBitmap = estadoLocal.fotoBitmap,
            clasificacion = clasificacion,
            mensajeEstado = clasificacion?.let { ClasificadorCloro.mensaje(it, rangos) }
                ?: (errorCampo ?: "Ingresa un valor entre 0,00 y 10,00 ppm"),
            rangos = rangos,
            controlesHoy = controles,
            guardando = estadoLocal.guardando,
            mensaje = estadoLocal.mensaje,
            registroExitoso = estadoLocal.registroExitoso
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

    /** Prepara el archivo temporal y Uri antes de invocar TakePicture */
    fun prepararCapturaFoto(context: Context): Uri {
        val (uri, archivo) = ImagenUtils.crearUriFotoTemporal(context)
        local.value = local.value.copy(fotoUri = uri, fotoRuta = archivo.absolutePath)
        return uri
    }

    /** Se llama cuando TakePicture devuelve true */
    fun confirmarFotoCapturada(context: Context) {
        val uri = local.value.fotoUri ?: return
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                ImagenUtils.cargarBitmapDesdeUri(context, uri)
            }
            local.value = local.value.copy(
                fotoBitmap = bitmap,
                mensaje = "Foto guardada exitosamente"
            )
        }
    }

    fun eliminarFoto() {
        local.value.fotoRuta?.let { ruta ->
            try { File(ruta).delete() } catch (_: Exception) {}
        }
        local.value = local.value.copy(fotoUri = null, fotoRuta = null, fotoBitmap = null)
    }

    fun ajustar(delta: Double) {
        val actual = Formato.parsear(local.value.valorTexto) ?: 0.0
        val nuevo = (Math.round((actual + delta) * 100.0) / 100.0).coerceIn(0.0, 10.0)
        local.value = local.value.copy(valorTexto = Formato.decimal(nuevo))
    }

    fun consumirMensaje() {
        local.value = local.value.copy(mensaje = null)
    }

    fun consumirRegistroExitoso() {
        local.value = local.value.copy(registroExitoso = false)
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
            var exito = false
            val resultadoMensaje = try {
                val nivel = repo.registrarMedicionManual(
                    puntoId = punto.puntoId,
                    valor = valor,
                    observacion = estado.observacion,
                    fotoRuta = estado.fotoRuta
                )
                exito = true
                val alerta = if (nivel.estado == EstadoCloro.NORMAL) "" else " Se generó/actualizó una alerta en Alertas."
                val conFoto = if (!estado.fotoRuta.isNullOrBlank()) " con foto guardada exitosamente." else "."
                "Medición registrada: ${Formato.ppm(nivel.valorPpm)} (${nivel.estado.etiqueta})$conFoto$alerta"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: "No se pudo registrar la medición."
            }
            local.value = if (exito) {
                local.value.copy(
                    guardando = false,
                    observacion = "",
                    fotoUri = null,
                    fotoRuta = null,
                    fotoBitmap = null,
                    mensaje = resultadoMensaje,
                    registroExitoso = true
                )
            } else {
                local.value.copy(
                    guardando = false,
                    mensaje = resultadoMensaje
                )
            }
        }
    }
}
