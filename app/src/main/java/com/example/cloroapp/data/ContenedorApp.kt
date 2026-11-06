package com.example.cloroapp.data

import android.content.Context
import com.example.cloroapp.data.remote.ApiFactory
import com.example.cloroapp.notificaciones.Notificador

/** Contenedor sencillo de dependencias: una sola instancia de base de datos, API y repositorios. */
class ContenedorApp(context: Context) {
    private val appContext = context.applicationContext

    val baseDatos: AppDatabase = AppDatabase.getDatabase(appContext)
    val preferencias = PreferenciasRepository(appContext)
    val repositorio = CloroRepository(
        context = appContext,
        db = baseDatos,
        api = ApiFactory.crearServicio(),
        prefs = preferencias,
        notificador = Notificador(appContext)
    )
}
