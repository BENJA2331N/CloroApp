package com.example.cloroapp.domain

/**
 * Rangos de cloro libre en ppm. Valores por defecto = propuesta de la respuesta 1 del docente:
 *  - Normal: 3,00 a 5,00
 *  - Advertencia: 2,00 a menos de 3,00, o sobre 5,00 hasta 6,00
 *  - Crítico: menos de 2,00 o más de 6,00
 * Son configurables (respuesta 4).
 */
data class RangosCloro(
    val criticoMin: Double = 2.0,
    val normalMin: Double = 3.0,
    val normalMax: Double = 5.0,
    val criticoMax: Double = 6.0
) {
    fun esValido(): Boolean =
        criticoMin > 0.0 && criticoMin < normalMin && normalMin < normalMax && normalMax < criticoMax
}

data class ResultadoClasificacion(
    val estado: EstadoCloro,
    /** Null cuando la medición es normal. */
    val desviacion: TipoDesviacion?
)

object ClasificadorCloro {

    /** Las mediciones se registran y muestran con dos decimales (respuesta 3). */
    fun redondear(ppm: Double): Double = Math.round(ppm * 100.0) / 100.0

    /** Clasificación sin huecos entre rangos: cada valor cae en exactamente un estado. */
    fun clasificar(ppm: Double, rangos: RangosCloro = RangosCloro()): ResultadoClasificacion {
        val v = redondear(ppm)
        return when {
            v < rangos.criticoMin -> ResultadoClasificacion(EstadoCloro.CRITICO, TipoDesviacion.INSUFICIENTE)
            v < rangos.normalMin -> ResultadoClasificacion(EstadoCloro.ADVERTENCIA, TipoDesviacion.INSUFICIENTE)
            v <= rangos.normalMax -> ResultadoClasificacion(EstadoCloro.NORMAL, null)
            v <= rangos.criticoMax -> ResultadoClasificacion(EstadoCloro.ADVERTENCIA, TipoDesviacion.EXCESIVO)
            else -> ResultadoClasificacion(EstadoCloro.CRITICO, TipoDesviacion.EXCESIVO)
        }
    }

    fun mensaje(resultado: ResultadoClasificacion, rangos: RangosCloro = RangosCloro()): String = when {
        resultado.estado == EstadoCloro.NORMAL ->
            "Nivel adecuado (${Formato.decimal(rangos.normalMin)} – ${Formato.decimal(rangos.normalMax)} ppm)"
        resultado.estado == EstadoCloro.ADVERTENCIA && resultado.desviacion == TipoDesviacion.INSUFICIENTE ->
            "Advertencia: cloro bajo el rango normal. Revisar la dosificación."
        resultado.estado == EstadoCloro.ADVERTENCIA ->
            "Advertencia: cloro sobre el rango normal. Revisar la dosificación."
        resultado.desviacion == TipoDesviacion.INSUFICIENTE ->
            "¡ALERTA CRÍTICA! Cloro insuficiente. Aplicar acción correctiva."
        else ->
            "¡ALERTA CRÍTICA! Cloro excesivo. Aplicar acción correctiva."
    }
}
