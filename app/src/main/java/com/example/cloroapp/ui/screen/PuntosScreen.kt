package com.example.cloroapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cloroapp.data.Usuario
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.ui.AppViewModelProvider
import com.example.cloroapp.ui.componentes.TarjetaDato
import com.example.cloroapp.ui.componentes.TarjetaPunto
import com.example.cloroapp.ui.componentes.paleta
import com.example.cloroapp.ui.viewmodel.PuntosViewModel
import com.example.cloroapp.ui.viewmodel.SesionViewModel

@Composable
fun PuntosScreen(
    usuario: Usuario,
    sesionVm: SesionViewModel,
    onAbrirPunto: (Long) -> Unit,
    vm: PuntosViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val puntos by vm.puntos.collectAsStateWithLifecycle()
    val estadoSync by sesionVm.estadoSync.collectAsStateWithLifecycle()
    val ultimaSync by sesionVm.ultimaSincronizacion.collectAsStateWithLifecycle()
    val agrupados = remember(puntos) { puntos.groupBy { it.granjaNombre } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Hola, ${usuario.nombre}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${usuario.rol.etiqueta} · Estado del cloro libre por punto de medición",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                for (estado in EstadoCloro.entries) {
                    TarjetaDato(
                        titulo = estado.etiqueta,
                        valor = puntos.count { it.estado == estado }.toString(),
                        color = estado.paleta().texto,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (ultimaSync > 0L) "Última actualización: ${Formato.fechaHora(ultimaSync)}"
                    else "Aún sin sincronizar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = sesionVm::sincronizarAhora, enabled = !estadoSync.sincronizando) {
                    if (estadoSync.sincronizando) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text("  Actualizar")
                }
            }
            if (estadoSync.mensaje != null) {
                Text(
                    text = estadoSync.mensaje ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        agrupados.forEach { (granja, lista) ->
            item(key = "granja-$granja") {
                Text(
                    text = granja,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(lista, key = { it.puntoId }) { punto ->
                TarjetaPunto(punto = punto, onClick = { onAbrirPunto(punto.puntoId) })
            }
        }

        if (puntos.isEmpty()) {
            item {
                Text(
                    text = "No hay puntos de medición disponibles para tu perfil.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
