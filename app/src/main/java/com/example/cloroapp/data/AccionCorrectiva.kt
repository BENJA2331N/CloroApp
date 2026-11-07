package com.example.cloroapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cloroapp.domain.EstadoResolucion

/** Acción correctiva con los datos pedidos en la respuesta 23: descripción, responsable, fecha y hora, punto, medición y estado. */
@Entity(
    tableName = "acciones_correctivas",
    foreignKeys = [
        ForeignKey(
            entity = Alerta::class,
            parentColumns = ["id"],
            childColumns = ["alertaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["alertaId"])]
)
data class AccionCorrectiva(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alertaId: Long,
    val puntoMedicionId: Long,
    val nivelCloroId: Long,
    val descripcion: String,
    val responsable: String,
    val fechaHoraMillis: Long,
    val estadoResolucion: EstadoResolucion = EstadoResolucion.REGISTRADA,
    val fotoBase64: String? = null
)
