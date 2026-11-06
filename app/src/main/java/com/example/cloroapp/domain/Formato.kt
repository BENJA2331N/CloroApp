package com.example.cloroapp.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formatos de presentación con configuración regional chilena (coma decimal, dos decimales). */
object Formato {
    private val local: Locale = Locale.forLanguageTag("es-CL")

    fun decimal(valor: Double): String = String.format(local, "%.2f", valor)

    fun ppm(valor: Double): String = String.format(local, "%.2f ppm", valor)

    fun fechaHora(millis: Long): String =
        SimpleDateFormat("dd-MM-yyyy HH:mm", local).format(Date(millis))

    fun hora(millis: Long): String = SimpleDateFormat("HH:mm", local).format(Date(millis))

    /** Acepta coma o punto como separador decimal. */
    fun parsear(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

    fun antiguedad(desdeMillis: Long, ahoraMillis: Long = System.currentTimeMillis()): String {
        val minutos = ((ahoraMillis - desdeMillis) / 60_000L).coerceAtLeast(0L)
        return when {
            minutos < 1L -> "hace instantes"
            minutos < 60L -> "hace $minutos min"
            minutos < 1_440L -> "hace ${minutos / 60L} h ${minutos % 60L} min"
            else -> "hace ${minutos / 1_440L} d ${(minutos % 1_440L) / 60L} h"
        }
    }
}
