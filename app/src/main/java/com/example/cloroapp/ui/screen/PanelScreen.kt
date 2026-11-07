package com.example.cloroapp.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cloroapp.data.EstadisticaPunto
import com.example.cloroapp.domain.ClasificadorCloro
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.domain.Periodo
import com.example.cloroapp.domain.RangosCloro
import com.example.cloroapp.notificaciones.Notificador
import com.example.cloroapp.ui.AppViewModelProvider
import com.example.cloroapp.ui.componentes.TarjetaDato
import com.example.cloroapp.ui.componentes.paleta
import com.example.cloroapp.ui.viewmodel.PanelViewModel

private data class ResumenGranja(
    val granjaId: Long,
    val nombre: String,
    val promedio: Double,
    val minimo: Double,
    val maximo: Double,
    val mediciones: Int,
    val desviaciones: Int
)

private fun resumirPorGranja(estadisticas: List<EstadisticaPunto>): List<ResumenGranja> =
    estadisticas.groupBy { it.granjaId }.map { (id, puntos) ->
        val total = puntos.sumOf { it.total }
        ResumenGranja(
            granjaId = id,
            nombre = puntos.first().granjaNombre,
            promedio = if (total == 0) 0.0 else puntos.sumOf { it.promedio * it.total } / total,
            minimo = puntos.minOf { it.minimo },
            maximo = puntos.maxOf { it.maximo },
            mediciones = total,
            desviaciones = puntos.sumOf { it.desviaciones }
        )
    }

/** Panel con mediciones actuales, alertas, promedios, barras animadas y exportación de reporte nativo. */
@Composable
fun PanelScreen(vm: PanelViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val periodo by vm.periodo.collectAsStateWithLifecycle()
    val granjaId by vm.granjaId.collectAsStateWithLifecycle()
    val estadisticas by vm.estadisticas.collectAsStateWithLifecycle()
    val puntosActuales by vm.puntosActuales.collectAsStateWithLifecycle()
    val alertasPendientes by vm.alertasPendientes.collectAsStateWithLifecycle()
    val rangos by vm.rangos.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    val resumenGranjas = remember(estadisticas) { resumirPorGranja(estadisticas) }
    val puntosFiltrados = remember(estadisticas, granjaId) {
        estadisticas.filter { granjaId == null || it.granjaId == granjaId }
    }
    val totalMediciones = estadisticas.sumOf { it.total }
    val totalDesviaciones = estadisticas.sumOf { it.desviaciones }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Panel de calidad del agua",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val reporte = buildString {
                            appendLine("Reporte Ejecutivo de Calidad del Agua — CloroApp")
                            appendLine("Período analizado: ${periodo.etiqueta}")
                            appendLine("Total mediciones: $totalMediciones | Desviaciones: $totalDesviaciones")
                            appendLine("Alertas pendientes activas: $alertasPendientes")
                            appendLine("---")
                            resumenGranjas.forEach { g ->
                                appendLine("${g.nombre}: Promedio ${Formato.ppm(g.promedio)} (mín ${Formato.decimal(g.minimo)}, máx ${Formato.decimal(g.maximo)})")
                            }
                        }
                        Notificador.compartirReporte(
                            context = contexto,
                            asunto = "Reporte CloroApp (${periodo.etiqueta})",
                            contenido = reporte
                        )
                    }
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Compartir reporte ejecutivo")
                }
            }
        }

        item {
            Text("Estado actual de los puntos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                for (estado in EstadoCloro.entries) {
                    TarjetaDato(
                        titulo = estado.etiqueta,
                        valor = puntosActuales.count { it.estado == estado }.toString(),
                        color = estado.paleta().texto,
                        modifier = Modifier.weight(1f)
                    )
                }
                TarjetaDato(
                    titulo = "Alertas pendientes",
                    valor = alertasPendientes.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text("Período de análisis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TarjetaDato("Mediciones", totalMediciones.toString(), Modifier.weight(1f))
                TarjetaDato("Desviaciones", totalDesviaciones.toString(), Modifier.weight(1f))
                TarjetaDato(
                    "% en desviación",
                    if (totalMediciones == 0) "—" else "${Formato.decimal(totalDesviaciones * 100.0 / totalMediciones)} %",
                    Modifier.weight(1f)
                )
            }
        }

        item {
            Text("Comparación entre granjas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(resumenGranjas, key = { "resumen-${it.granjaId}" }) { granja ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(granja.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    BarraPromedio(valor = granja.promedio, rangos = rangos)
                    Text(
                        text = "Promedio ${Formato.ppm(granja.promedio)} · mín ${Formato.decimal(granja.minimo)} · " +
                            "máx ${Formato.decimal(granja.maximo)} · ${granja.desviaciones} de ${granja.mediciones} fuera de rango",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text("Detalle por punto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                FilterChip(selected = granjaId == null, onClick = { vm.cambiarGranja(null) }, label = { Text("Todas") })
                for (granja in resumenGranjas) {
                    FilterChip(
                        selected = granjaId == granja.granjaId,
                        onClick = { vm.cambiarGranja(granja.granjaId) },
                        label = { Text(granja.nombre.removePrefix("Granja ")) }
                    )
                }
            }
        }

        items(puntosFiltrados, key = { it.puntoId }) { punto ->
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${punto.codigo} · ${punto.galpon}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Prom. ${Formato.decimal(punto.promedio)} ppm",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                BarraPromedio(valor = punto.promedio, rangos = rangos)
                Text(
                    text = "Mín ${Formato.decimal(punto.minimo)} · Máx ${Formato.decimal(punto.maximo)} · " +
                        "${punto.desviaciones} de ${punto.total} fuera de rango",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (estadisticas.isEmpty()) {
            item {
                Text(
                    text = "No hay mediciones en el período seleccionado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Barra horizontal animada y coloreada según el estado que corresponde al valor promedio (IE 2.2.2). */
@Composable
private fun BarraPromedio(valor: Double, rangos: RangosCloro) {
    val maximo = (rangos.criticoMax + 1.0).toFloat()
    val fraccionObjetivo = (valor.toFloat() / maximo).coerceIn(0.02f, 1f)
    val fraccionAnimada by animateFloatAsState(
        targetValue = fraccionObjetivo,
        animationSpec = tween(durationMillis = 500),
        label = "barraPromedioFraccion"
    )
    val colorObjetivo = ClasificadorCloro.clasificar(valor, rangos).estado.paleta().texto
    val colorAnimado by animateColorAsState(
        targetValue = colorObjetivo,
        animationSpec = tween(durationMillis = 400),
        label = "barraPromedioColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraccionAnimada)
                .fillMaxHeight()
                .background(colorAnimado)
        )
    }
}
