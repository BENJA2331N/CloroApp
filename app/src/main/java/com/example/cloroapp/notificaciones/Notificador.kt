package com.example.cloroapp.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cloroapp.MainActivity
import com.example.cloroapp.R

/**
 * Integración de recursos nativos del dispositivo (IE 2.4.1):
 * 1. Notificaciones locales del sistema con canales de alta prioridad (NotificationManager).
 * 2. Motor de vibración háptica nativa (Vibrator / VibratorManager) para alertas críticas y advertencias.
 * 3. Sistema nativo de compartición de reportes (Intent.ACTION_SEND / Chooser).
 */
class Notificador(private val context: Context) {

    fun mostrarAlertaCritica(alertaId: Long, titulo: String, texto: String) {
        vibrarAlertaCritica()

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

    /** Activa un patrón de vibración nativo en el teléfono cuando se registra una desviación crítica. */
    fun vibrarAlertaCritica() {
        try {
            val vibrador = obtenerVibrador() ?: return
            if (!vibrador.hasVibrator()) return
            val patron = longArrayOf(0L, 250L, 120L, 350L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrador.vibrate(VibrationEffect.createWaveform(patron, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrador.vibrate(patron, -1)
            }
        } catch (_: Exception) {
            // En emuladores o equipos sin motor háptico se ignora de forma segura.
        }
    }

    /** Pulso háptico corto de confirmación al guardar un control válido. */
    fun vibrarConfirmacion() {
        try {
            val vibrador = obtenerVibrador() ?: return
            if (!vibrador.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrador.vibrate(VibrationEffect.createOneShot(70L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrador.vibrate(70L)
            }
        } catch (_: Exception) {
        }
    }

    private fun obtenerVibrador(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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
                    enableVibration(true)
                }
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(canal)
            }
        }

        /** Abre la hoja nativa de Android (Sharesheet) para compartir un reporte de alerta o calidad del agua. */
        fun compartirReporte(context: Context, asunto: String, contenido: String) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, asunto)
                putExtra(Intent.EXTRA_TEXT, contenido)
            }
            val chooser = Intent.createChooser(sendIntent, "Compartir reporte de CloroApp").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }
}
