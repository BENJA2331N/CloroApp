package com.example.cloroapp.ui.screen

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cloroapp.data.ImagenUtils
import com.example.cloroapp.data.Usuario
import com.example.cloroapp.domain.ClasificadorCloro
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.domain.OrigenMedicion
import com.example.cloroapp.domain.Periodo
import com.example.cloroapp.ui.AppViewModelProvider
import com.example.cloroapp.ui.componentes.EncabezadoDetalle
import com.example.cloroapp.ui.componentes.EtiquetaEstado
import com.example.cloroapp.ui.componentes.GraficoCloro
import com.example.cloroapp.ui.componentes.TarjetaDato
import com.example.cloroapp.ui.componentes.paleta
import com.example.cloroapp.ui.viewmodel.PuntoDetalleViewModel

@Composable
fun PuntoDetalleScreen(
    puntoId: Long,
    usuario: Usuario,
    onVolver: () -> Unit,
    onRegistrar: (Long) -> Unit,
    vm: PuntoDetalleViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    LaunchedEffect(puntoId) { vm.seleccionarPunto(puntoId) }

    val punto by vm.punto.collectAsStateWithLifecycle()
    val historial by vm.historial.collectAsStateWithLifecycle()
    val periodo by vm.periodo.collectAsStateWithLifecycle()
    val rangos by vm.rangos.collectAsStateWithLifecycle()

    val serie = remember(historial) { historial.asReversed().map { it.fechaHoraMillis to it.valorPpm } }
    val promedio = remember(historial) { if (historial.isEmpty()) null else historial.map { it.valorPpm }.average() }
    val minimo = remember(historial) { historial.minOfOrNull { it.valorPpm } }
    val maximo = remember(historial) { historial.maxOfOrNull { it.valorPpm } }
    val desviaciones = remember(historial) { historial.count { it.estado != EstadoCloro.NORMAL } }

    Column(modifier = Modifier.fillMaxSize()) {
        EncabezadoDetalle(
            titulo = punto?.let { "${it.codigo} · ${it.galpon}" } ?: "Punto de medición",
            onVolver = onVolver
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            punto?.let { p ->
                item {
                    val estado = p.estado
                    val paleta = estado?.paleta()
                    val colorTexto = paleta?.texto ?: MaterialTheme.colorScheme.onSurfaceVariant
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = paleta?.fondo ?: MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${p.granjaNombre} · ${p.tipo.etiqueta}",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorTexto
                            )
                            Text(
                                text = p.valorPpm?.let { Formato.ppm(it) } ?: "Sin mediciones",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = colorTexto
                            )
                            if (estado != null) {
                                EtiquetaEstado(estado)
                                Text(
                                    text = ClasificadorCloro.mensaje(
                                        ClasificadorCloro.clasificar(p.valorPpm ?: 0.0, rangos), rangos
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorTexto,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            p.fechaHoraMillis?.let {
                                Text(
                                    text = "Actualizado: ${Formato.fechaHora(it)} (${Formato.antiguedad(it)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorTexto
                                )
                            }
                        }
                    }
                }

                if (usuario.rol.puedeRegistrarMediciones) {
                    item {
                        Button(onClick = { onRegistrar(p.puntoId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Registrar medición en este punto")
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (opcion in Periodo.entries) {
                        FilterChip(
                            selected = periodo == opcion,
                            onClick = { vm.cambiarPeriodo(opcion) },
                            label = { Text(opcion.etiqueta) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Historial de cloro libre (ppm)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                GraficoCloro(serie = serie, rangos = rangos)
                Text(
                    text = "Banda verde: rango normal ${Formato.decimal(rangos.normalMin)} – " +
                        "${Formato.decimal(rangos.normalMax)} ppm. Líneas rojas: límites críticos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TarjetaDato("Promedio", promedio?.let { Formato.decimal(it) } ?: "—", Modifier.weight(1f))
                    TarjetaDato("Mínimo", minimo?.let { Formato.decimal(it) } ?: "—", Modifier.weight(1f))
                    TarjetaDato("Máximo", maximo?.let { Formato.decimal(it) } ?: "—", Modifier.weight(1f))
                    TarjetaDato("Desviaciones", desviaciones.toString(), Modifier.weight(1f))
                }
            }

            item {
                Text(
                    text = "Últimas mediciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(historial.take(40), key = { it.id }) { medicion ->
                val bitmapGuardado = remember(medicion.fotoBase64) {
                    ImagenUtils.base64ABitmap(medicion.fotoBase64)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Formato.fechaHora(medicion.fechaHoraMillis),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val origen = if (medicion.origen == OrigenMedicion.MANUAL) {
                                "${medicion.registradoPor}" + if (medicion.sincronizado) "" else " · pendiente de sincronizar"
                            } else {
                                medicion.origen.etiqueta
                            }
                            Text(
                                text = origen,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            medicion.observacion?.let { nota ->
                                Text(
                                    text = "Nota: $nota",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = Formato.ppm(medicion.valorPpm),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = medicion.estado.paleta().texto,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        EtiquetaEstado(medicion.estado)
                    }
                    if (bitmapGuardado != null) {
                        Image(
                            bitmap = bitmapGuardado.asImageBitmap(),
                            contentDescription = "Fotografía del fotómetro guardada en SQLite",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
                HorizontalDivider()
            }

            if (historial.size > 40) {
                item {
                    Text(
                        text = "Se muestran las 40 mediciones más recientes de ${historial.size} en el período.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
