package com.example.cloroapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cloroapp.domain.Rol

/**
 * Usuario ficticio. Inicia sesión con correo y contraseña (respuesta 16): no se usa RUT
 * ni credenciales corporativas reales. Un rol principal por usuario (respuesta 15).
 */
@Entity(tableName = "usuarios", indices = [Index(value = ["correo"], unique = true)])
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val correo: String,
    val contrasenaHash: String,
    val rol: Rol
)

/** Granjas asignadas a un usuario (respuestas 14 y 15). */
@Entity(
    tableName = "usuario_granja",
    primaryKeys = ["usuarioId", "granjaId"],
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Granja::class,
            parentColumns = ["id"],
            childColumns = ["granjaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["granjaId"])]
)
data class UsuarioGranja(
    val usuarioId: Long,
    val granjaId: Long
)
