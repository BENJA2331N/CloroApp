package com.example.cloroapp

import com.example.cloroapp.data.remote.SimuladorCloro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** El simulador sigue la propuesta de las respuestas 10 y 11: 3 granjas x 4 puntos, un dato cada 5 minutos. */
class SimuladorCloroTest {

    private val ahora = 1_790_000_000_000L

    @Test
    fun hay12PuntosConCodigosUnicos() {
        assertEquals(12, SimuladorCloro.puntos.size)
        assertEquals(12, SimuladorCloro.puntos.map { it.codigo }.toSet().size)
        assertEquals(3, SimuladorCloro.puntos.map { it.granja }.toSet().size)
    }

    @Test
    fun losDatosSonDeterministas() {
        assertEquals(SimuladorCloro.ultimas(ahora), SimuladorCloro.ultimas(ahora + 1_000L))
    }

    @Test
    fun lasUltimasMedicionesEstanAlineadasACadaCincoMinutos() {
        val ultimas = SimuladorCloro.ultimas(ahora)
        assertEquals(12, ultimas.size)
        assertTrue(ultimas.all { it.fechaHoraMillis % SimuladorCloro.INTERVALO_MS == 0L })
    }

    @Test
    fun elHistorialDe30DiasTieneUnDatoPorHoraYPunto() {
        val historial = SimuladorCloro.historial(dias = 30, ahoraMs = ahora)
        assertEquals(30 * 24 * 12, historial.size)
        assertTrue(historial.all { it.valorPpm in 0.0..8.0 })
    }
}
