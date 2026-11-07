package com.example.cloroapp.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.ui.AppViewModelProvider
import com.example.cloroapp.ui.componentes.EncabezadoDetalle
import com.example.cloroapp.ui.componentes.SelectorDesplegable
import com.example.cloroapp.ui.componentes.paleta
import com.example.cloroapp.ui.viewmodel.RegistroViewModel
import kotlinx.coroutines.launch

/** Registro de un control de cloro en terreno, con TakePicture + FileProvider y Room. */
@Composable
fun RegistrarScreen(
    puntoIdInicial: Long,
    snackbar: SnackbarHostState,
    onVolver: (() -> Unit)? = null,
    vm: RegistroViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by vm.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    var uriDestinoCamara by remember { mutableStateOf<Uri?>(null) }

    // Recurso nativo robusto: ActivityResultContracts.TakePicture con FileProvider
    val lanzadorTakePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) {
            vm.confirmarFotoCapturada(contexto)
        }
    }

    val lanzadorPermisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            val uri = vm.prepararCapturaFoto(contexto)
            uriDestinoCamara = uri
            lanzadorTakePicture.launch(uri)
        } else {
            scope.launch {
                snackbar.showSnackbar("Permiso de cámara denegado. Puedes registrar la medición sin foto.")
            }
        }
    }

    LaunchedEffect(puntoIdInicial) {
        if (puntoIdInicial > 0L) vm.seleccionarPunto(puntoIdInicial)
    }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            scope.launch { snackbar.showSnackbar(it) }
            vm.consumirMensaje()
        }
    }
    LaunchedEffect(estado.registroExitoso) {
        if (estado.registroExitoso) {
            vm.consumirRegistroExitoso()
            onVolver?.invoke()
        }
    }

    val estadoCloro = estado.clasificacion?.estado ?: EstadoCloro.NORMAL
    val paleta = estadoCloro.paleta()
    val colorTarjeta by animateColorAsState(
        targetValue = paleta.fondo,
        animationSpec = tween(durationMillis = 350),
        label = "colorTarjeta"
    )
    val colorTexto by animateColorAsState(
        targetValue = paleta.texto,
        animationSpec = tween(durationMillis = 350),
        label = "colorTexto"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onVolver != null) {
            EncabezadoDetalle(titulo = "Medición de verificación", onVolver = onVolver)
        } else {
            Text(
                text = "Registrar control de cloro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        SelectorDesplegable(
            etiqueta = "Selecciona un punto de medición",
            seleccionado = estado.puntoSeleccionado,
            opciones = estado.puntos,
            textoDe = { "${it.granjaNombre} · ${it.codigo} (${it.galpon})" },
            onSeleccion = { vm.seleccionarPunto(it.puntoId) }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colorTarjeta),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nivel de cloro libre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorTexto
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedContent(
                    targetState = estado.valor?.let { Formato.ppm(it) } ?: "— ppm",
                    label = "animValorPpm"
                ) { textoValor ->
                    Text(
                        text = textoValor,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorTexto
                    )
                }
                Text(
                    text = "Estado: ${estadoCloro.etiqueta.uppercase()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = estado.mensajeEstado,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = colorTexto
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { vm.ajustar(-0.1) }, shape = RoundedCornerShape(12.dp)) {
                        Text("- 0,1", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { vm.ajustar(0.1) }, shape = RoundedCornerShape(12.dp)) {
                        Text("+ 0,1", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        OutlinedTextField(
            value = estado.valorTexto,
            onValueChange = vm::cambiarTexto,
            label = { Text("Valor medido en fotómetro (ppm)") },
            leadingIcon = {
                Icon(
                    imageVector = if (estado.errorValor != null) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (estado.errorValor != null) MaterialTheme.colorScheme.error else paleta.texto
                )
            },
            trailingIcon = {
                if (estado.valorTexto.isNotEmpty()) {
                    IconButton(onClick = { vm.cambiarTexto("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Limpiar valor")
                    }
                }
            },
            isError = estado.errorValor != null,
            supportingText = {
                Text(
                    text = estado.errorValor
                        ?: "Rango operativo del instrumento: 0,00 a 10,00 ppm (2 decimales)",
                    color = if (estado.errorValor != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = estado.observacion,
            onValueChange = vm::cambiarObservacion,
            label = { Text("Nota de terreno (opcional)") },
            placeholder = { Text("Ej. Purga previa realizada en bebedero") },
            leadingIcon = { Icon(Icons.Filled.Create, contentDescription = null) },
            supportingText = { Text("${estado.observacion.length}/120 caracteres") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Botón de cámara con FileProvider y TakePicture
        OutlinedButton(
            onClick = {
                val tienePermiso = ContextCompat.checkSelfPermission(
                    contexto,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (tienePermiso) {
                    val uri = vm.prepararCapturaFoto(contexto)
                    uriDestinoCamara = uri
                    lanzadorTakePicture.launch(uri)
                } else {
                    lanzadorPermisoCamara.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(
                text = if (estado.fotoBitmap == null) "  Tomar foto del fotómetro (Cámara)"
                else "  Volver a tomar fotografía"
            )
        }

        AnimatedVisibility(
            visible = estado.fotoBitmap != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            estado.fotoBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Foto capturada (se guardará con el control)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = vm::eliminarFoto) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar foto")
                            }
                        }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Fotografía del fotómetro",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }
        }

        Button(
            onClick = vm::registrar,
            enabled = !estado.guardando && estado.valor != null && estado.puntoSeleccionado != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = paleta.texto),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text("  Registrar control en terreno", color = Color.White)
        }

        Text(
            text = "Rangos vigentes: normal ${Formato.decimal(estado.rangos.normalMin)} – " +
                "${Formato.decimal(estado.rangos.normalMax)} ppm · crítico bajo ${Formato.decimal(estado.rangos.criticoMin)} " +
                "o sobre ${Formato.decimal(estado.rangos.criticoMax)} ppm.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Controles registrados hoy: ${estado.controlesHoy}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
