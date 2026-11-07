package com.example.cloroapp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cloroapp.ui.viewmodel.SesionUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    estado: SesionUiState,
    onCambioCorreo: (String) -> Unit,
    onCambioContrasena: (String) -> Unit,
    onSeleccionarDemo: (String) -> Unit,
    onIniciarSesion: (correo: String, contrasena: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarContrasena by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CloroApp",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Monitoreo y control de cloro libre en línea",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = estado.correo,
                    onValueChange = onCambioCorreo,
                    label = { Text("Correo institucional") },
                    placeholder = { Text("usuario@cloroapp.cl") },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = "Correo")
                    },
                    trailingIcon = {
                        if (estado.errorCorreo != null) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = "Error en correo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else if (estado.correo.isNotEmpty()) {
                            IconButton(onClick = { onCambioCorreo("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Limpiar correo")
                            }
                        }
                    },
                    isError = estado.errorCorreo != null,
                    supportingText = {
                        Text(
                            text = estado.errorCorreo ?: "Ingresa tu cuenta asignada (@cloroapp.cl)",
                            color = if (estado.errorCorreo != null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = estado.contrasena,
                    onValueChange = onCambioContrasena,
                    label = { Text("Contraseña") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = "Contraseña")
                    },
                    trailingIcon = {
                        TextButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                            Text(
                                text = if (mostrarContrasena) "Ocultar" else "Ver",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    },
                    isError = estado.errorContrasena != null,
                    supportingText = {
                        Text(
                            text = estado.errorContrasena ?: "Mínimo 6 caracteres alfanuméricos",
                            color = if (estado.errorContrasena != null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = estado.error != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = estado.error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { onIniciarSesion(estado.correo, estado.contrasena) },
                    enabled = !estado.iniciando,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (estado.iniciando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Iniciar sesión")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Acceso rápido de demostración (rellena automáticamente):",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            AssistChip(
                onClick = { onSeleccionarDemo("operario1@cloroapp.cl") },
                label = { Text("Operario") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            AssistChip(
                onClick = { onSeleccionarDemo("supervisor1@cloroapp.cl") },
                label = { Text("Supervisor") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            AssistChip(
                onClick = { onSeleccionarDemo("jefatura1@cloroapp.cl") },
                label = { Text("Jefatura") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Cuentas demo: operario1..9 · supervisor1..4 · jefatura1..2 @cloroapp.cl\nContraseña: cloro123",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
