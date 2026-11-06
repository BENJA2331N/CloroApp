package com.example.cloroapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cloroapp.domain.TipoPunto

/** Datos que identifican un punto de medición según la respuesta 7: granja, galpón, código, ubicación y tipo. */
@Entity(
    tableName = "puntos_medicion",
    foreignKeys = [
        ForeignKey(
            entity = Granja::class,
            parentColumns = ["id"],
            childColumns = ["granjaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["granjaId"]),
        Index(value = ["codigo"], unique = true)
    ]
)
data class PuntoMedicion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val granjaId: Long,
    val galpon: String,
    val codigo: String,
    val ubicacion: String,
    val tipo: TipoPunto
)
