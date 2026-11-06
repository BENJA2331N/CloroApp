package com.example.cloroapp.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.ui.AppViewModelProvider
import com.example.cloroapp.ui.componentes.SelectorDesplegable
import com.example.cloroapp.ui.componentes.paleta
import com.example.cloroapp.ui.viewmodel.RegistroViewModel

/** Registro de un control de cloro en terreno, con vista previa del estado según los rangos vigentes. */
@Composable
fun RegistrarScreen(
    puntoIdInicial: Long,
    snackbar: SnackbarHostState,
    vm: RegistroViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(puntoIdInicial) {
        if (puntoIdInicial > 0L) vm.seleccionarPunto(puntoIdInicial)
    }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.consumirMensaje()
        }
    }

    val estadoCloro = estado.clasificacion?.estado ?: EstadoCloro.NORMAL
    val paleta = estadoCloro.paleta()
    val colorTarjeta by animateColorAsState(targetValue = paleta.fondo, label = "colorTarjeta")
    val colorTexto by animateColorAsState(targetValue = paleta.texto, label = "colorTexto")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Registrar control de cloro",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        SelectorDesplegable(
            etiqueta = "Selecciona un punto de medición",
            seleccionado = estado.puntoSeleccionado,
            opciones = estado.puntos,
            textoDe = { "${it.granjaNombre} · ${it.codigo} (${it.galpon})" },
            onSeleccion = { vm.seleccionarPunto(it.puntoId) }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = estado.valor?.let { Formato.ppm(it) } ?: "— ppm",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
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
            label = { Text("Valor medido (ppm)") },
            isError = estado.valor == null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = vm::registrar,
            enabled = !estado.guardando && estado.valor != null && estado.puntoSeleccionado != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = paleta.texto),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Registrar control", color = Color.White)
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
