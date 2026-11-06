package com.example.cloroapp.domain

/** Perfiles de usuario definidos en el caso (respuesta 13 del docente). */
enum class Rol(val etiqueta: String) {
    OPERARIO("Operario"),
    SUPERVISOR("Supervisor"),
    JEFATURA("Jefatura");

    /** Operarios y supervisores registran controles; la jefatura solo consulta. */
    val puedeRegistrarMediciones: Boolean get() = this != JEFATURA

    /** Solo el supervisor valida y cierra alertas (respuestas 21 y 22). */
    val puedeCerrarAlertas: Boolean get() = this == SUPERVISOR

    /** Los rangos son configurables (respuesta 4); un operario no puede cambiarlos. */
    val puedeConfigurarRangos: Boolean get() = this != OPERARIO
}

enum class EstadoCloro(val etiqueta: String) {
    NORMAL("Normal"),
    ADVERTENCIA("Advertencia"),
    CRITICO("Crítico")
}

/** Una desviación puede ser por cloro insuficiente o excesivo (respuesta 5). */
enum class TipoDesviacion(val etiqueta: String) {
    INSUFICIENTE("Cloro insuficiente"),
    EXCESIVO("Cloro excesivo")
}

enum class EstadoAlerta(val etiqueta: String) {
    ABIERTA("Abierta"),
    EN_GESTION("En gestión"),
    CERRADA("Cerrada")
}

enum class EstadoResolucion(val etiqueta: String) {
    REGISTRADA("Registrada"),
    VALIDADA("Validada por supervisor")
}

enum class OrigenMedicion(val etiqueta: String) {
    API("Sistema de monitoreo"),
    MANUAL("Control manual")
}

/** Tipos de punto citados en la respuesta 4: tanque, línea de distribución o bebedero. */
enum class TipoPunto(val etiqueta: String) {
    TANQUE("Tanque"),
    LINEA_DISTRIBUCION("Línea de distribución"),
    BEBEDERO("Bebedero")
}

/** Períodos de consulta de historial recomendados en la respuesta 27. */
enum class Periodo(val etiqueta: String, val horas: Long) {
    H24("24 horas", 24L),
    D7("7 días", 7L * 24L),
    D30("30 días", 30L * 24L);

    fun desdeMillis(ahora: Long = System.currentTimeMillis()): Long = ahora - horas * 3_600_000L
}
