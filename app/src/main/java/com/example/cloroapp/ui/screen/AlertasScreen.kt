package com.example.cloroapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cloroapp.data.AlertaDetalle
import com.example.cloroapp.domain.EstadoAlerta
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.ui.AppViewModelProvider
import com.example.cloroapp.ui.componentes.EtiquetaEstado
import com.example.cloroapp.ui.componentes.paleta
import com.example.cloroapp.ui.viewmodel.AlertasViewModel

@Composable
fun AlertasScreen(
    onAbrirAlerta: (Long) -> Unit,
    vm: AlertasViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val alertas by vm.alertas.collectAsStateWithLifecycle()
    val soloPendientes by vm.soloPendientes.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Alertas",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                FilterChip(
                    selected = soloPendientes,
                    onClick = { vm.cambiarFiltro(true) },
                    label = { Text("Pendientes") }
                )
                FilterChip(
                    selected = !soloPendientes,
                    onClick = { vm.cambiarFiltro(false) },
                    label = { Text("Historial (todas)") }
                )
            }
        }

        if (alertas.isEmpty()) {
            item {
                Text(
                    text = if (soloPendientes) "No hay alertas pendientes. Todo en orden." else "Aún no hay alertas registradas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(alertas, key = { it.alerta.id }) { detalle ->
            TarjetaAlerta(detalle = detalle, onClick = { onAbrirAlerta(detalle.alerta.id) })
        }
    }
}

@Composable
private fun TarjetaAlerta(detalle: AlertaDetalle, onClick: () -> Unit) {
    val alerta = detalle.alerta
    val cerrada = alerta.estado == EstadoAlerta.CERRADA
    val paleta = alerta.severidad.paleta()
    val colorTexto = if (cerrada) MaterialTheme.colorScheme.onSurfaceVariant else paleta.texto

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cerrada) MaterialTheme.colorScheme.surfaceVariant else paleta.fondo
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${alerta.tipo.etiqueta} · ${detalle.codigoPunto}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto,
                    modifier = Modifier.weight(1f)
                )
                EtiquetaEstado(alerta.severidad)
            }
            Text(
                text = "${detalle.granjaNombre} · ${detalle.galpon}",
                style = MaterialTheme.typography.bodySmall,
                color = colorTexto
            )
            Text(
                text = "${Formato.ppm(alerta.valorPpm)} · ${Formato.fechaHora(alerta.fechaHoraMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                color = colorTexto
            )
            Text(
                text = if (cerrada) {
                    "Cerrada por ${alerta.cerradaPor ?: "—"}"
                } else {
                    "${alerta.estado.etiqueta} · ${Formato.antiguedad(alerta.fechaHoraMillis)}"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = colorTexto
            )
        }
    }
}
