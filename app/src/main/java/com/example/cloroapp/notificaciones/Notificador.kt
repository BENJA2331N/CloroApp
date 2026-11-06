package com.example.cloroapp.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cloroapp.MainActivity
import com.example.cloroapp.R

/** Notificaciones locales para alertas críticas (respuesta 26: para el MVP bastan notificaciones locales). */
class Notificador(private val context: Context) {

    fun mostrarAlertaCritica(alertaId: Long, titulo: String, texto: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendiente = PendingIntent.getActivity(
            context,
            alertaId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_ALERTAS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendiente)
            .build()

        try {
            manager.notify(alertaId.toInt(), notificacion)
        } catch (e: SecurityException) {
            // Sin permiso POST_NOTIFICATIONS: la alerta igualmente queda visible dentro de la app.
        }
    }

    companion object {
        const val CANAL_ALERTAS = "alertas_cloro"

        fun crearCanal(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canal = NotificationChannel(
                    CANAL_ALERTAS,
                    "Alertas de cloro",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos cuando el cloro libre sale del rango crítico"
                }
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(canal)
            }
        }
    }
}
