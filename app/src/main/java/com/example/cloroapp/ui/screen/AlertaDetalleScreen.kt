package com.example.cloroapp.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cloroapp.data.Usuario
import com.example.cloroapp.domain.EstadoAlerta
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.domain.TipoDesviacion
import com.example.cloroapp.domain.ValidadorFormularios
import com.example.cloroapp.notificaciones.Notificador
import com.example.cloroapp.ui.AppViewModelProvider
import com.example.cloroapp.ui.componentes.EncabezadoDetalle
import com.example.cloroapp.ui.componentes.EtiquetaEstado
import com.example.cloroapp.ui.componentes.paleta
import com.example.cloroapp.ui.viewmodel.AlertaDetalleViewModel
import kotlinx.coroutines.launch

@Composable
fun AlertaDetalleScreen(
    alertaId: Long,
    usuario: Usuario,
    snackbar: SnackbarHostState,
    onVolver: () -> Unit,
    onRegistrarMedicion: (puntoId: Long) -> Unit,
    vm: AlertaDetalleViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    LaunchedEffect(alertaId) { vm.seleccionar(alertaId) }

    val detalle by vm.alerta.collectAsStateWithLifecycle()
    val acciones by vm.acciones.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val descripcion by vm.descripcionAccion.collectAsStateWithLifecycle()
    val errorDescripcion by vm.errorDescripcion.collectAsStateWithLifecycle()
    val fotoEvidencia by vm.fotoEvidencia.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) vm.adjuntarFotoEvidencia(bitmap)
    }

    val lanzadorPermisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            lanzadorCamara.launch()
        } else {
            scope.launch { snackbar.showSnackbar("Permiso de cámara no concedido.") }
        }
    }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbar.showSnackbar(it)
            vm.consumirMensaje()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val d = detalle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                EncabezadoDetalle(titulo = "Detalle de alerta", onVolver = onVolver)
            }
            if (d != null) {
                IconButton(
                    onClick = {
                        val a = d.alerta
                        val resumen = buildString {
                            appendLine("Reporte de Alerta — CloroApp")
                            appendLine("Punto: ${d.codigoPunto} (${d.granjaNombre} · ${d.galpon})")
                            appendLine("Desviación: ${a.tipo.etiqueta} (${a.severidad.etiqueta})")
                            appendLine("Valor medido: ${Formato.ppm(a.valorPpm)} el ${Formato.fechaHora(a.fechaHoraMillis)}")
                            appendLine("Estado: ${a.estado.etiqueta}")
                            appendLine("Acciones registradas: ${acciones.size}")
                        }
                        Notificador.compartirReporte(
                            context = contexto,
                            asunto = "Alerta ${d.codigoPunto} - ${a.tipo.etiqueta}",
                            contenido = resumen
                        )
                    }
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Compartir reporte de alerta")
                }
            }
        }

        if (d == null) {
            Text(
                text = "Cargando alerta…",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }

        val alerta = d.alerta
        val paleta = alerta.severidad.paleta()
        val cerrada = alerta.estado == EstadoAlerta.CERRADA

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = paleta.fondo)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${alerta.tipo.etiqueta} · ${d.codigoPunto}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = paleta.texto
                        )
                        EtiquetaEstado(alerta.severidad)
                        Text(
                            text = "${d.granjaNombre} · ${d.galpon}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = paleta.texto
                        )
                        Text(
                            text = "Medición: ${Formato.ppm(alerta.valorPpm)} el ${Formato.fechaHora(alerta.fechaHoraMillis)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = paleta.texto
                        )
                        Text(
                            text = if (cerrada) {
                                "Cerrada por ${alerta.cerradaPor ?: "—"}" +
                                    (alerta.fechaCierreMillis?.let { " el ${Formato.fechaHora(it)}" } ?: "")
                            } else {
                                "Estado: ${alerta.estado.etiqueta} · pendiente ${Formato.antiguedad(alerta.fechaHoraMillis)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = paleta.texto
                        )
                    }
                }
            }

            if (!cerrada) {
                item {
                    Text(
                        text = "Procedimiento recomendado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = procedimientoPara(alerta.tipo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!cerrada && usuario.rol.puedeRegistrarMediciones) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.animateContentSize()
                    ) {
                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = vm::actualizarDescripcion,
                            label = { Text("Acción correctiva realizada") },
                            placeholder = { Text("Ej. Se ajustó bomba dosificadora y se purgó la línea por 5 min") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (errorDescripcion != null) Icons.Filled.Warning else Icons.Filled.Build,
                                    contentDescription = null,
                                    tint = if (errorDescripcion != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (descripcion.isNotEmpty()) {
                                    IconButton(onClick = { vm.actualizarDescripcion("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Limpiar descripción")
                                    }
                                }
                            },
                            isError = errorDescripcion != null,
                            supportingText = {
                                Text(
                                    text = errorDescripcion
                                        ?: "Mínimo ${ValidadorFormularios.MIN_CARACTERES_ACCION} caracteres (${descripcion.trim().length}/${ValidadorFormularios.MAX_CARACTERES_ACCION})",
                                    color = if (errorDescripcion != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedButton(
                            onClick = {
                                val tienePermiso = ContextCompat.checkSelfPermission(
                                    contexto,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (tienePermiso) lanzadorCamara.launch()
                                else lanzadorPermisoCamara.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(
                                if (fotoEvidencia == null) "  Adjuntar fotografía de respaldo (Cámara)"
                                else "  Cambiar fotografía de respaldo"
                            )
                        }

                        AnimatedVisibility(
                            visible = fotoEvidencia != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            fotoEvidencia?.let { bmp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Evidencia adjunta", style = MaterialTheme.typography.labelMedium)
                                            IconButton(onClick = { vm.adjuntarFotoEvidencia(null) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Quitar evidencia")
                                            }
                                        }
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Evidencia de acción correctiva",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { vm.registrarAccion() },
                            enabled = ValidadorFormularios.validarAccionCorrectiva(descripcion) == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Text("  Registrar acción correctiva")
                        }
                        OutlinedButton(
                            onClick = { onRegistrarMedicion(alerta.puntoMedicionId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Registrar nueva medición de verificación")
                        }
                    }
                }
            }

            if (!cerrada && usuario.rol.puedeCerrarAlertas) {
                item {
                    Button(onClick = vm::cerrarAlerta, modifier = Modifier.fillMaxWidth()) {
                        Text("Validar y cerrar alerta")
                    }
                    Text(
                        text = "Para cerrar: acción correctiva registrada y una nueva medición dentro del rango normal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                Text(
                    text = "Acciones correctivas (${acciones.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (acciones.isEmpty()) {
                    Text(
                        text = "Aún no se registran acciones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(acciones, key = { it.id }) { accion ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = accion.descripcion, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${accion.responsable} · ${Formato.fechaHora(accion.fechaHoraMillis)} · " +
                                accion.estadoResolucion.etiqueta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Box(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier) { content() }
}

/** Las acciones correctivas difieren según el tipo de desviación (respuesta 5). */
private fun procedimientoPara(tipo: TipoDesviacion): String = when (tipo) {
    TipoDesviacion.INSUFICIENTE ->
        "1. Informar al supervisor.\n2. Verificar la medición.\n3. Revisar la dosificación: nivel de solución de cloro y bomba dosificadora.\n" +
            "4. Documentar la acción correctiva.\n5. Realizar una nueva medición."
    TipoDesviacion.EXCESIVO ->
        "1. Informar al supervisor.\n2. Verificar la medición.\n3. Reducir la dosificación y purgar la línea de agua.\n" +
            "4. Documentar la acción correctiva.\n5. Realizar una nueva medición."
}
