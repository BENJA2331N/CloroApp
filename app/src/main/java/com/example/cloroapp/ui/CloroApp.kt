package com.example.cloroapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cloroapp.data.Usuario
import com.example.cloroapp.domain.Rol
import com.example.cloroapp.ui.screen.AlertaDetalleScreen
import com.example.cloroapp.ui.screen.AlertasScreen
import com.example.cloroapp.ui.screen.LoginScreen
import com.example.cloroapp.ui.screen.PanelScreen
import com.example.cloroapp.ui.screen.PerfilScreen
import com.example.cloroapp.ui.screen.PuntoDetalleScreen
import com.example.cloroapp.ui.screen.PuntosScreen
import com.example.cloroapp.ui.screen.RegistrarScreen
import com.example.cloroapp.ui.viewmodel.SesionViewModel

private data class Pestana(val ruta: String, val titulo: String, val icono: ImageVector)

private val PestanaPuntos = Pestana("puntos", "Puntos", Icons.Filled.Place)
private val PestanaAlertas = Pestana("alertas", "Alertas", Icons.Filled.Warning)
private val PestanaRegistrar = Pestana("registrar", "Registrar", Icons.Filled.Add)
private val PestanaPanel = Pestana("panel", "Panel", Icons.Filled.Home)
private val PestanaPerfil = Pestana("perfil", "Perfil", Icons.Filled.Person)

/** Cada perfil ve solo las secciones que le corresponden según la respuesta 13 del docente. */
private fun pestanasPara(rol: Rol): List<Pestana> = when (rol) {
    Rol.OPERARIO -> listOf(PestanaPuntos, PestanaRegistrar, PestanaAlertas, PestanaPerfil)
    Rol.SUPERVISOR -> listOf(PestanaPuntos, PestanaAlertas, PestanaRegistrar, PestanaPanel, PestanaPerfil)
    Rol.JEFATURA -> listOf(PestanaPanel, PestanaPuntos, PestanaAlertas, PestanaPerfil)
}

@Composable
fun CloroApp(sesionVm: SesionViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val sesion by sesionVm.sesion.collectAsStateWithLifecycle()

    Crossfade(
        targetState = sesion.cargando to sesion.usuario,
        animationSpec = tween(durationMillis = 300),
        label = "transicionSesion"
    ) { (cargando, usuario) ->
        when {
            cargando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            usuario == null -> {
                Scaffold { padding ->
                    LoginScreen(
                        estado = sesion,
                        onCambioCorreo = sesionVm::actualizarCorreo,
                        onCambioContrasena = sesionVm::actualizarContrasena,
                        onSeleccionarDemo = sesionVm::completarCredencialesDemo,
                        onIniciarSesion = sesionVm::iniciarSesion,
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            else -> ContenidoPrincipal(usuario = usuario, sesionVm = sesionVm)
        }
    }
}

@Composable
private fun ContenidoPrincipal(usuario: Usuario, sesionVm: SesionViewModel) {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route?.substringBefore('?')
    val snackbar = remember { SnackbarHostState() }
    val alertasPendientes by sesionVm.alertasPendientes.collectAsStateWithLifecycle()

    val pestanas = remember(usuario.rol) { pestanasPara(usuario.rol) }
    val inicio = pestanas.first().ruta

    SolicitarPermisoNotificaciones()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            // La barra inferior solo se muestra en las secciones principales, no en los detalles.
            if (pestanas.any { it.ruta == rutaActual }) {
                NavigationBar {
                    pestanas.forEach { pestana ->
                        NavigationBarItem(
                            selected = rutaActual == pestana.ruta,
                            onClick = {
                                val volvioAPestana = navController.popBackStack(pestana.ruta, inclusive = false)
                                if (!volvioAPestana) {
                                    navController.navigate(pestana.ruta) {
                                        popUpTo(inicio) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                if (pestana.ruta == PestanaAlertas.ruta && alertasPendientes > 0) {
                                    BadgedBox(badge = { Badge { Text(alertasPendientes.toString()) } }) {
                                        Icon(pestana.icono, contentDescription = null)
                                    }
                                } else {
                                    Icon(pestana.icono, contentDescription = null)
                                }
                            },
                            label = { Text(pestana.titulo) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = inicio,
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(250)) },
            popEnterTransition = { fadeIn(animationSpec = tween(250)) },
            popExitTransition = { fadeOut(animationSpec = tween(250)) },
            modifier = Modifier.padding(padding)
        ) {
            composable("puntos") {
                PuntosScreen(
                    usuario = usuario,
                    sesionVm = sesionVm,
                    onAbrirPunto = { navController.navigate("punto/$it") }
                )
            }
            composable(
                route = "punto/{puntoId}",
                arguments = listOf(navArgument("puntoId") { type = NavType.LongType })
            ) { entrada ->
                PuntoDetalleScreen(
                    puntoId = entrada.arguments?.getLong("puntoId") ?: -1L,
                    usuario = usuario,
                    onVolver = { navController.popBackStack() },
                    onRegistrar = { navController.navigate("registrar?puntoId=$it") }
                )
            }
            composable(
                route = "registrar?puntoId={puntoId}",
                arguments = listOf(
                    navArgument("puntoId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { entrada ->
                val puntoIdArg = entrada.arguments?.getLong("puntoId") ?: -1L
                RegistrarScreen(
                    puntoIdInicial = puntoIdArg,
                    snackbar = snackbar,
                    onVolver = if (puntoIdArg > 0L) ({ navController.popBackStack() }) else null
                )
            }
            composable("alertas") {
                AlertasScreen(onAbrirAlerta = { navController.navigate("alerta/$it") })
            }
            composable(
                route = "alerta/{alertaId}",
                arguments = listOf(navArgument("alertaId") { type = NavType.LongType })
            ) { entrada ->
                AlertaDetalleScreen(
                    alertaId = entrada.arguments?.getLong("alertaId") ?: -1L,
                    usuario = usuario,
                    snackbar = snackbar,
                    onVolver = { navController.popBackStack() },
                    onRegistrarMedicion = { navController.navigate("registrar?puntoId=$it") }
                )
            }
            composable("panel") { PanelScreen() }
            composable("perfil") { PerfilScreen(usuario = usuario, sesionVm = sesionVm) }
        }
    }
}

/** Android 13+ exige pedir permiso para mostrar las notificaciones de alertas críticas. */
@Composable
private fun SolicitarPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val contexto = LocalContext.current
        val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(Unit) {
            val concedido = ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!concedido) lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
