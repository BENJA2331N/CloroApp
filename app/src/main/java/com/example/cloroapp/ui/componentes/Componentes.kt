package com.example.cloroapp.ui.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cloroapp.data.PuntoResumen
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.domain.RangosCloro

/** Colores de cada estado. Fondo y texto se eligen juntos para mantener el contraste en tema claro y oscuro. */
data class PaletaEstado(val fondo: Color, val texto: Color)

fun EstadoCloro.paleta(): PaletaEstado = when (this) {
    EstadoCloro.NORMAL -> PaletaEstado(Color(0xFFE8F5E9), Color(0xFF1B5E20))
    EstadoCloro.ADVERTENCIA -> PaletaEstado(Color(0xFFFFF8E1), Color(0xFF8D5A00))
    EstadoCloro.CRITICO -> PaletaEstado(Color(0xFFFFEBEE), Color(0xFFB71C1C))
}

@Composable
fun EtiquetaEstado(estado: EstadoCloro, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = estado.paleta().texto) {
        Text(
            text = estado.etiqueta.uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TarjetaPunto(punto: PuntoResumen, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val estado = punto.estado
    val paleta = estado?.paleta()
    val colorTexto by animateColorAsState(
        targetValue = paleta?.texto ?: MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "colorTextoPunto"
    )
    val colorFondo by animateColorAsState(
        targetValue = paleta?.fondo ?: MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "colorFondoPunto"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondo)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${punto.codigo} · ${punto.galpon}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
                Text(
                    text = "${punto.tipo.etiqueta} · ${punto.ubicacion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorTexto
                )
                Text(
                    text = punto.fechaHoraMillis?.let { "Actualizado: ${Formato.fechaHora(it)}" } ?: "Sin mediciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorTexto
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = punto.valorPpm?.let { Formato.ppm(it) } ?: "— ppm",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
                if (estado != null) EtiquetaEstado(estado)
            }
        }
    }
}

@Composable
fun TarjetaDato(titulo: String, valor: String, modifier: Modifier = Modifier, color: Color? = null) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EncabezadoDetalle(titulo: String, onVolver: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onVolver) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun <T> SelectorDesplegable(
    etiqueta: String,
    seleccionado: T?,
    opciones: List<T>,
    textoDe: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandido by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expandido = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = seleccionado?.let(textoDe) ?: etiqueta,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
            Text("▼")
        }
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoDe(opcion)) },
                    onClick = {
                        expandido = false
                        onSeleccion(opcion)
                    }
                )
            }
        }
    }
}

/**
 * Gráfico de líneas del historial de un punto. La banda verde marca el rango normal y las líneas
 * punteadas los límites críticos. [serie] debe venir ordenada de la medición más antigua a la más reciente.
 */
@Composable
fun GraficoCloro(
    serie: List<Pair<Long, Double>>,
    rangos: RangosCloro,
    modifier: Modifier = Modifier
) {
    if (serie.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay datos suficientes para mostrar el gráfico.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val colorLinea = MaterialTheme.colorScheme.primary
    val colorBanda = Color(0xFF2E7D32).copy(alpha = 0.15f)
    val colorLimite = Color(0xFFB71C1C).copy(alpha = 0.7f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val maximoY = maxOf(rangos.criticoMax + 1.0, serie.maxOf { it.second } + 0.5).toFloat()
        val minimoX = serie.first().first
        val rangoX = (serie.last().first - minimoX).coerceAtLeast(1L).toFloat()

        fun posX(tiempo: Long): Float = (tiempo - minimoX) / rangoX * size.width
        fun posY(valor: Double): Float = size.height - (valor.toFloat() / maximoY) * size.height

        drawRect(
            color = colorBanda,
            topLeft = Offset(0f, posY(rangos.normalMax)),
            size = Size(size.width, posY(rangos.normalMin) - posY(rangos.normalMax))
        )

        val guiones = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
        for (limite in listOf(rangos.criticoMin, rangos.criticoMax)) {
            drawLine(
                color = colorLimite,
                start = Offset(0f, posY(limite)),
                end = Offset(size.width, posY(limite)),
                strokeWidth = 1.5f,
                pathEffect = guiones
            )
        }

        val trazo = Path()
        serie.forEachIndexed { indice, (tiempo, valor) ->
            val x = posX(tiempo)
            val y = posY(valor)
            if (indice == 0) trazo.moveTo(x, y) else trazo.lineTo(x, y)
        }
        drawPath(
            path = trazo,
            color = colorLinea,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

