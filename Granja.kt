package com.example.cloroapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "granjas", indices = [Index(value = ["nombre"], unique = true)])
data class Granja(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val ubicacion: String
)
