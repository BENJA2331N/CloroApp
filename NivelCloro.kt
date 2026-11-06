package com.example.cloroapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.OrigenMedicion

/**
 * Medición de cloro libre en ppm. Los registros son inmutables: no existe update ni delete
 * en el DAO, por lo que se conservan las mediciones originales (respuesta 17).
 * Una medición manual queda con [sincronizado] = false hasta enviarse a la API (respuesta 12).
 */
@Entity(
    tableName = "niveles_cloro",
    foreignKeys = [
        ForeignKey(
            entity = PuntoMedicion::class,
            parentColumns = ["id"],
            childColumns = ["puntoMedicionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["puntoMedicionId", "fechaHoraMillis"], unique = true),
        Index(value = ["sincronizado"])
    ]
)
data class NivelCloro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val puntoMedicionId: Long,
    val valorPpm: Double,
    val fechaHoraMillis: Long,
    val estado: EstadoCloro,
    val registradoPor: String,
    val origen: OrigenMedicion = OrigenMedicion.MANUAL,
    val sincronizado: Boolean = false
)
