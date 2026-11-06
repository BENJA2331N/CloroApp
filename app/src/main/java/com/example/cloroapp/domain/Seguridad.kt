package com.example.cloroapp.domain

import java.security.MessageDigest

/** Protección básica de credenciales (respuesta 33): nunca se guarda la contraseña en texto plano. */
object Seguridad {
    fun hash(correo: String, contrasena: String): String {
        val resumen = MessageDigest.getInstance("SHA-256")
            .digest("${correo.trim().lowercase()}:$contrasena".toByteArray(Charsets.UTF_8))
        return resumen.joinToString("") { "%02x".format(it) }
    }
}
