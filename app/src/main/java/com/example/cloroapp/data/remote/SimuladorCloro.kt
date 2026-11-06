package com.example.cloroapp.data.remote

import com.example.cloroapp.domain.ClasificadorCloro
import com.example.cloroapp.domain.TipoPunto
import kotlin.math.sin
import kotlin.random.Random

data class PuntoSimulado(
    val granja: String,
    val ubicacionGranja: String,
    val codigo: String,
    val galpon: String,
    val ubicacion: String,
    val tipo: TipoPunto
)

/**
 * Generador determinista de datos ficticios (respuestas 8, 10 y 11):
 * 3 granjas x 4 puntos = 12 puntos, una medición cada 5 minutos.
 * Para un mismo punto e instante siempre entrega el mismo valor, de modo que el historial es estable.
 */
object SimuladorCloro {
    const val INTERVALO_MS = 5L * 60L * 1000L
    private const val BUCKETS_POR_EPISODIO = 6L // ventanas de 30 min
    private const val PROBABILIDAD_DESVIACION = 0.12

    private val granjas = listOf(
        Triple("Granja Melipilla", "Melipilla, Región Metropolitana", "MEL"),
        Triple("Granja Pomaire", "Pomaire, Región Metropolitana", "POM"),
        Triple("Granja San Pedro", "San Pedro, Región Metropolitana", "SPE")
    )

    private val plantillaPuntos = listOf(
        Triple("Galpón 1", "Salida de tanque de agua", TipoPunto.TANQUE),
        Triple("Galpón 2", "Línea de distribución principal", TipoPunto.LINEA_DISTRIBUCION),
        Triple("Galpón 3", "Extremo de línea de bebederos", TipoPunto.BEBEDERO),
        Triple("Galpón 4", "Extremo de línea de bebederos", TipoPunto.BEBEDERO)
    )

    val puntos: List<PuntoSimulado> = granjas.flatMap { (granja, ubicacionGranja, prefijo) ->
        plantillaPuntos.mapIndexed { indice, (galpon, ubicacion, tipo) ->
            PuntoSimulado(granja, ubicacionGranja, "$prefijo-0${indice + 1}", galpon, ubicacion, tipo)
        }
    }

    fun puntosDto(): List<PuntoDto> = puntos.map {
        PuntoDto(it.codigo, it.granja, it.ubicacionGranja, it.galpon, it.ubicacion, it.tipo.name)
    }

    fun valorEn(codigo: String, instanteMs: Long): Double {
        val bucket = instanteMs / INTERVALO_MS
        val episodio = bucket / BUCKETS_POR_EPISODIO
        val semilla = codigo.hashCode().toLong() * 1_000_003L

        val azarEpisodio = Random(semilla + episodio)
        val desvio = if (azarEpisodio.nextDouble() < PROBABILIDAD_DESVIACION) {
            val magnitud = 1.0 + azarEpisodio.nextDouble() * 2.2
            if (azarEpisodio.nextBoolean()) magnitud else -magnitud
        } else {
            0.0
        }

        val ruido = (Random(semilla * 31L + bucket).nextDouble() - 0.5) * 0.4
        val deriva = 0.4 * sin(bucket / 40.0 + (codigo.hashCode() % 7))
        return ClasificadorCloro.redondear((4.0 + deriva + ruido + desvio).coerceIn(0.0, 8.0))
    }

    /** Última medición de cada punto (instante alineado a los 5 minutos). */
    fun ultimas(ahoraMs: Long): List<MedicionDto> {
        val instante = ahoraMs / INTERVALO_MS * INTERVALO_MS
        return puntos.map { MedicionDto(it.codigo, valorEn(it.codigo, instante), instante) }
    }

    /** Historial de todos los puntos con un dato por hora (por defecto), de más antiguo a más reciente. */
    fun historial(dias: Int, ahoraMs: Long, pasoMs: Long = 3_600_000L): List<MedicionDto> {
        val fin = ahoraMs / pasoMs * pasoMs
        val cantidad = dias * 24L * 3_600_000L / pasoMs
        val resultado = ArrayList<MedicionDto>((cantidad * puntos.size).toInt())
        for (k in cantidad - 1 downTo 0L) {
            val t = fin - k * pasoMs
            for (p in puntos) resultado.add(MedicionDto(p.codigo, valorEn(p.codigo, t), t))
        }
        return resultado
    }
}
