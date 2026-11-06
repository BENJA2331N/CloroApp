package com.example.cloroapp

import android.app.Application
import android.util.Log
import com.example.cloroapp.data.ContenedorApp
import com.example.cloroapp.notificaciones.Notificador
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CloroApplication : Application() {

    lateinit var contenedor: ContenedorApp
        private set

    private val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        contenedor = ContenedorApp(this)
        Notificador.crearCanal(this)

        alcanceApp.launch {
            try {
                contenedor.repositorio.asegurarDatosIniciales()
            } catch (e: Exception) {
                Log.e("CloroApp", "No se pudieron cargar los datos iniciales", e)
            }
        }
    }
}
