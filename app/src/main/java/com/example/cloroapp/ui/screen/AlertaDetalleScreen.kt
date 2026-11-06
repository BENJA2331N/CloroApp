package com.example.cloroapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cloroapp.data.Usuario
import com.example.cloroapp.domain.EstadoAlerta
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.domain.TipoDesviacion
import com.example.cloroapp.ui.AppViewModelProvider
import com.example.cloroapp.ui.componentes.EncabezadoDetalle
import com.example.cloroapp.ui.componentes.EtiquetaEstado
import com.example.cloroapp.ui.componentes.paleta
import com.example.cloroapp.ui.viewmodel.AlertaDetalleViewModel

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
    var descripcion by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbar.showSnackbar(it)
            vm.consumirMensaje()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        EncabezadoDetalle(titulo = "Detalle de alerta", onVolver = onVolver)

        val d = detalle
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
            modifier = Modifier.fillMaxSize(),
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it },
                            label = { Text("Acción correctiva realizada") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                vm.registrarAccion(descripcion)
                                descripcion = ""
                            },
                            enabled = descripcion.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Registrar acción correctiva")
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

/** Las acciones correctivas difieren según el tipo de desviación (respuesta 5). */
private fun procedimientoPara(tipo: TipoDesviacion): String = when (tipo) {
    TipoDesviacion.INSUFICIENTE ->
        "1. Informar al supervisor.\n2. Verificar la medición.\n3. Revisar la dosificación: nivel de solución de cloro y bomba dosificadora.\n" +
            "4. Documentar la acción correctiva.\n5. Realizar una nueva medición."
    TipoDesviacion.EXCESIVO ->
        "1. Informar al supervisor.\n2. Verificar la medición.\n3. Reducir la dosificación y purgar la línea de agua.\n" +
            "4. Documentar la acción correctiva.\n5. Realizar una nueva medición."
}
