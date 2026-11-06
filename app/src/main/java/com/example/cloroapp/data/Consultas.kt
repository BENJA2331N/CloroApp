package com.example.cloroapp.data

import androidx.room.Embedded
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.TipoPunto

/** Punto de medición junto a su última medición (valor y estado nulos si aún no hay datos). */
data class PuntoResumen(
    val puntoId: Long,
    val codigo: String,
    val galpon: String,
    val tipo: TipoPunto,
    val ubicacion: String,
    val granjaId: Long,
    val granjaNombre: String,
    val valorPpm: Double?,
    val estado: EstadoCloro?,
    val fechaHoraMillis: Long?
)

/** Alerta con los datos del punto y la granja para mostrarla en listas. */
data class AlertaDetalle(
    @Embedded val alerta: Alerta,
    val codigoPunto: String,
    val galpon: String,
    val granjaId: Long,
    val granjaNombre: String
)

/** Promedios, mínimos, máximos y desviaciones por punto en un período (respuesta 28). */
data class EstadisticaPunto(
    val puntoId: Long,
    val codigo: String,
    val galpon: String,
    val granjaId: Long,
    val granjaNombre: String,
    val promedio: Double,
    val minimo: Double,
    val maximo: Double,
    val total: Int,
    val desviaciones: Int
)
