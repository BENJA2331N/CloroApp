package com.example.cloroapp.data

import com.example.cloroapp.domain.Rol

/** Usuario ficticio de demostración. [granjas] son índices en la lista de granjas del simulador. */
data class UsuarioSemilla(
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val granjas: List<Int>
)

/**
 * Datos ficticios de la respuesta 34: 15 usuarios entre operarios, supervisores y jefaturas.
 * Todos usan la misma clave de demostración.
 */
object DatosIniciales {
    const val CLAVE_DEMO = "cloro123"

    val usuarios: List<UsuarioSemilla> = listOf(
        UsuarioSemilla("Marcela Soto", "jefatura1@cloroapp.cl", Rol.JEFATURA, emptyList()),
        UsuarioSemilla("Rodrigo Pérez", "jefatura2@cloroapp.cl", Rol.JEFATURA, emptyList()),

        UsuarioSemilla("Camila Rojas", "supervisor1@cloroapp.cl", Rol.SUPERVISOR, listOf(0)),
        UsuarioSemilla("Felipe Muñoz", "supervisor2@cloroapp.cl", Rol.SUPERVISOR, listOf(1)),
        UsuarioSemilla("Daniela Vargas", "supervisor3@cloroapp.cl", Rol.SUPERVISOR, listOf(2)),
        UsuarioSemilla("Andrés Fuentes", "supervisor4@cloroapp.cl", Rol.SUPERVISOR, listOf(0, 1)),

        UsuarioSemilla("Luis Contreras", "operario1@cloroapp.cl", Rol.OPERARIO, listOf(0)),
        UsuarioSemilla("Patricia Núñez", "operario2@cloroapp.cl", Rol.OPERARIO, listOf(0)),
        UsuarioSemilla("Jorge Araya", "operario3@cloroapp.cl", Rol.OPERARIO, listOf(0)),
        UsuarioSemilla("Carolina Silva", "operario4@cloroapp.cl", Rol.OPERARIO, listOf(1)),
        UsuarioSemilla("Matías Herrera", "operario5@cloroapp.cl", Rol.OPERARIO, listOf(1)),
        UsuarioSemilla("Valentina Díaz", "operario6@cloroapp.cl", Rol.OPERARIO, listOf(1)),
        UsuarioSemilla("Sebastián Lagos", "operario7@cloroapp.cl", Rol.OPERARIO, listOf(2)),
        UsuarioSemilla("Francisca Mora", "operario8@cloroapp.cl", Rol.OPERARIO, listOf(2)),
        UsuarioSemilla("Cristian Tapia", "operario9@cloroapp.cl", Rol.OPERARIO, listOf(2))
    )
}
