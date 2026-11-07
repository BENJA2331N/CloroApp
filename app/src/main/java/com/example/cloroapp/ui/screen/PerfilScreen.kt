package com.example.cloroapp.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cloroapp.data.Granja
import com.example.cloroapp.data.Usuario
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.domain.RangosCloro
import com.example.cloroapp.domain.ValidadorFormularios
import com.example.cloroapp.ui.viewmodel.SesionViewModel

@Composable
fun PerfilScreen(usuario: Usuario, sesionVm: SesionViewModel) {
    val rangos by sesionVm.rangos.collectAsStateWithLifecycle()
    val notificaciones by sesionVm.notificacionesActivas.collectAsStateWithLifecycle()
    val sinConexion by sesionVm.simularSinConexion.collectAsStateWithLifecycle()
    val pendientes by sesionVm.pendientesSincronizacion.collectAsStateWithLifecycle()
    val ultimaSync by sesionVm.ultimaSincronizacion.collectAsStateWithLifecycle()
    val estadoSync by sesionVm.estadoSync.collectAsStateWithLifecycle()

    var granjas by remember { mutableStateOf(emptyList<Granja>()) }
    var mostrarRangos by remember { mutableStateOf(false) }

    LaunchedEffect(usuario.id) { granjas = sesionVm.obtenerGranjas() }

    val lanzadorPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val contexto = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(usuario.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${usuario.rol.etiqueta} · ${usuario.correo}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (granjas.isEmpty()) "Sin granjas asignadas"
                    else "Granjas: " + granjas.joinToString(", ") { it.nombre },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text("Sincronización", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = (if (ultimaSync > 0L) "Última sincronización: ${Formato.fechaHora(ultimaSync)}. " else "Aún sin sincronizar. ") +
                "Controles pendientes de envío: $pendientes.",
            style = MaterialTheme.typography.bodyMedium
        )
        AnimatedVisibility(visible = estadoSync.mensaje != null) {
            Text(
                text = estadoSync.mensaje ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = sesionVm::sincronizarAhora,
            enabled = !estadoSync.sincronizando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (estadoSync.sincronizando) "Sincronizando…" else "Sincronizar ahora")
        }
        FilaInterruptor(
            titulo = "Simular sin conexión",
            descripcion = "Modo demostración: los controles quedan guardados en el teléfono y se envían al volver la conexión.",
            activado = sinConexion,
            onCambio = sesionVm::cambiarSimularSinConexion
        )

        Text("Notificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FilaInterruptor(
            titulo = "Alertas críticas",
            descripcion = "Aviso en el teléfono cuando un punto de tus granjas queda en estado crítico.",
            activado = notificaciones,
            onCambio = { activar ->
                sesionVm.cambiarNotificaciones(activar)
                if (activar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    lanzadorPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )

        Text("Rangos de cloro libre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "Normal: ${Formato.decimal(rangos.normalMin)} – ${Formato.decimal(rangos.normalMax)} ppm\n" +
                "Advertencia: ${Formato.decimal(rangos.criticoMin)} a menos de ${Formato.decimal(rangos.normalMin)} ppm, " +
                "o sobre ${Formato.decimal(rangos.normalMax)} hasta ${Formato.decimal(rangos.criticoMax)} ppm\n" +
                "Crítico: bajo ${Formato.decimal(rangos.criticoMin)} ppm o sobre ${Formato.decimal(rangos.criticoMax)} ppm",
            style = MaterialTheme.typography.bodyMedium
        )
        if (usuario.rol.puedeConfigurarRangos) {
            OutlinedButton(onClick = { mostrarRangos = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Build, contentDescription = null)
                Text("  Configurar rangos")
            }
        }

        OutlinedButton(onClick = sesionVm::cerrarSesion, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Text("  Cerrar sesión")
        }
    }

    if (mostrarRangos) {
        DialogoRangos(
            actual = rangos,
            onGuardar = sesionVm::guardarRangos,
            onRestablecer = sesionVm::restablecerRangos,
            onCerrar = { mostrarRangos = false }
        )
    }
}

@Composable
private fun FilaInterruptor(
    titulo: String,
    descripcion: String,
    activado: Boolean,
    onCambio: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = activado, onCheckedChange = onCambio)
    }
}

@Composable
private fun DialogoRangos(
    actual: RangosCloro,
    onGuardar: (RangosCloro) -> Boolean,
    onRestablecer: () -> Unit,
    onCerrar: () -> Unit
) {
    var criticoMin by remember { mutableStateOf(Formato.decimal(actual.criticoMin)) }
    var normalMin by remember { mutableStateOf(Formato.decimal(actual.normalMin)) }
    var normalMax by remember { mutableStateOf(Formato.decimal(actual.normalMax)) }
    var criticoMax by remember { mutableStateOf(Formato.decimal(actual.criticoMax)) }

    val validacion = remember(criticoMin, normalMin, normalMax, criticoMax) {
        ValidadorFormularios.validarRangos(criticoMin, normalMin, normalMax, criticoMax)
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Rangos de cloro libre (ppm)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoRango("Crítico si es menor que", criticoMin, validacion.errorCriticoMin) { criticoMin = it }
                CampoRango("Normal desde", normalMin, validacion.errorNormalMin) { normalMin = it }
                CampoRango("Normal hasta", normalMax, validacion.errorNormalMax) { normalMax = it }
                CampoRango("Crítico si es mayor que", criticoMax, validacion.errorCriticoMax) { criticoMax = it }
                Text(
                    text = validacion.errorGeneral
                        ?: "Deben ir de menor a mayor. Entre un límite crítico y el rango normal el estado es advertencia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (validacion.errorGeneral != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = validacion.esValido,
                onClick = {
                    val rangos = validacion.rangosValidos ?: return@TextButton
                    if (onGuardar(rangos)) onCerrar()
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onRestablecer()
                    onCerrar()
                }) { Text("Restablecer") }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        }
    )
}

@Composable
private fun CampoRango(
    etiqueta: String,
    valor: String,
    errorCampo: String?,
    onCambio: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        leadingIcon = {
            Icon(
                imageVector = if (errorCampo != null) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (errorCampo != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        isError = errorCampo != null,
        supportingText = errorCampo?.let { msg ->
            { Text(msg, color = MaterialTheme.colorScheme.error) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}
