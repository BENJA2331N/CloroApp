package com.example.cloroapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cloroapp.domain.RangosCloro
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "preferencias_cloro")

/** Preferencias locales: sesión activa, rangos configurables y opciones de la app. */
class PreferenciasRepository(private val context: Context) {

    private object Claves {
        val SESION_USUARIO = longPreferencesKey("sesion_usuario_id")
        val CRITICO_MIN = doublePreferencesKey("rango_critico_min")
        val NORMAL_MIN = doublePreferencesKey("rango_normal_min")
        val NORMAL_MAX = doublePreferencesKey("rango_normal_max")
        val CRITICO_MAX = doublePreferencesKey("rango_critico_max")
        val NOTIFICACIONES = booleanPreferencesKey("notificaciones_activas")
        val SIMULAR_SIN_CONEXION = booleanPreferencesKey("simular_sin_conexion")
        val ULTIMA_SINCRONIZACION = longPreferencesKey("ultima_sincronizacion")
    }

    val sesionUsuarioId: Flow<Long?> = context.dataStore.data.map { it[Claves.SESION_USUARIO] }

    val rangos: Flow<RangosCloro> = context.dataStore.data.map { pref ->
        val porDefecto = RangosCloro()
        RangosCloro(
            criticoMin = pref[Claves.CRITICO_MIN] ?: porDefecto.criticoMin,
            normalMin = pref[Claves.NORMAL_MIN] ?: porDefecto.normalMin,
            normalMax = pref[Claves.NORMAL_MAX] ?: porDefecto.normalMax,
            criticoMax = pref[Claves.CRITICO_MAX] ?: porDefecto.criticoMax
        )
    }

    val notificacionesActivas: Flow<Boolean> =
        context.dataStore.data.map { it[Claves.NOTIFICACIONES] ?: true }

    val simularSinConexion: Flow<Boolean> =
        context.dataStore.data.map { it[Claves.SIMULAR_SIN_CONEXION] ?: false }

    val ultimaSincronizacion: Flow<Long> =
        context.dataStore.data.map { it[Claves.ULTIMA_SINCRONIZACION] ?: 0L }

    suspend fun guardarSesion(usuarioId: Long) {
        context.dataStore.edit { it[Claves.SESION_USUARIO] = usuarioId }
    }

    suspend fun cerrarSesion() {
        context.dataStore.edit { it.remove(Claves.SESION_USUARIO) }
    }

    suspend fun guardarRangos(rangos: RangosCloro) {
        context.dataStore.edit { pref ->
            pref[Claves.CRITICO_MIN] = rangos.criticoMin
            pref[Claves.NORMAL_MIN] = rangos.normalMin
            pref[Claves.NORMAL_MAX] = rangos.normalMax
            pref[Claves.CRITICO_MAX] = rangos.criticoMax
        }
    }

    suspend fun restablecerRangos() {
        context.dataStore.edit { pref ->
            pref.remove(Claves.CRITICO_MIN)
            pref.remove(Claves.NORMAL_MIN)
            pref.remove(Claves.NORMAL_MAX)
            pref.remove(Claves.CRITICO_MAX)
        }
    }

    suspend fun guardarNotificaciones(activadas: Boolean) {
        context.dataStore.edit { it[Claves.NOTIFICACIONES] = activadas }
    }

    suspend fun guardarSimularSinConexion(activado: Boolean) {
        context.dataStore.edit { it[Claves.SIMULAR_SIN_CONEXION] = activado }
    }

    suspend fun guardarUltimaSincronizacion(millis: Long) {
        context.dataStore.edit { it[Claves.ULTIMA_SINCRONIZACION] = millis }
    }
}
