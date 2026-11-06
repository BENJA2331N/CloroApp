package com.example.cloroapp.data.remote

/** Punto de medición tal como lo entrega la API. */
data class PuntoDto(
    val codigo: String,
    val granja: String,
    val ubicacionGranja: String,
    val galpon: String,
    val ubicacion: String,
    val tipo: String
)

/** Medición de cloro libre tal como la entrega (o recibe) la API. */
data class MedicionDto(
    val codigoPunto: String,
    val valorPpm: Double,
    val fechaHoraMillis: Long
)
