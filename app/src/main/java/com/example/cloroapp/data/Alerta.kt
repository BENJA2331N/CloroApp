package com.example.cloroapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cloroapp.domain.EstadoAlerta
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.TipoDesviacion

/**
 * Alerta generada automáticamente cuando una medición sale de rango (respuesta 19).
 * [severidad] es ADVERTENCIA o CRITICO; [tipo] distingue cloro insuficiente de excesivo (respuesta 5).
 */
@Entity(
    tableName = "alertas",
    foreignKeys = [
        ForeignKey(
            entity = PuntoMedicion::class,
            parentColumns = ["id"],
            childColumns = ["puntoMedicionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NivelCloro::class,
            parentColumns = ["id"],
            childColumns = ["nivelCloroId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["puntoMedicionId"]), Index(value = ["nivelCloroId"])]
)
data class Alerta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val puntoMedicionId: Long,
    val nivelCloroId: Long,
    val tipo: TipoDesviacion,
    val severidad: EstadoCloro,
    val valorPpm: Double,
    val fechaHoraMillis: Long,
    val estado: EstadoAlerta = EstadoAlerta.ABIERTA,
    val cerradaPor: String? = null,
    val fechaCierreMillis: Long? = null
)
