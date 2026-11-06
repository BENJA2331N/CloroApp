package com.example.cloroapp

import com.example.cloroapp.domain.ClasificadorCloro
import com.example.cloroapp.domain.EstadoCloro
import com.example.cloroapp.domain.Formato
import com.example.cloroapp.domain.RangosCloro
import com.example.cloroapp.domain.TipoDesviacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica los rangos propuestos en la respuesta 1 del docente (normal 3–5 ppm, etc.). */
class ClasificadorCloroTest {

    private fun estado(ppm: Double) = ClasificadorCloro.clasificar(ppm).estado

    @Test
    fun normal_entre3y5ppm_incluyendoLosExtremos() {
        listOf(3.0, 3.5, 4.0, 5.0).forEach { assertEquals(EstadoCloro.NORMAL, estado(it)) }
        assertNull(ClasificadorCloro.clasificar(4.0).desviacion)
    }

    @Test
    fun advertencia_entre2yMenosDe3ppm() {
        listOf(2.0, 2.5, 2.99).forEach {
            val r = ClasificadorCloro.clasificar(it)
            assertEquals(EstadoCloro.ADVERTENCIA, r.estado)
            assertEquals(TipoDesviacion.INSUFICIENTE, r.desviacion)
        }
    }

    @Test
    fun advertencia_sobre5yHasta6ppm() {
        listOf(5.01, 5.5, 6.0).forEach {
            val r = ClasificadorCloro.clasificar(it)
            assertEquals(EstadoCloro.ADVERTENCIA, r.estado)
            assertEquals(TipoDesviacion.EXCESIVO, r.desviacion)
        }
    }

    @Test
    fun critico_bajo2ppm_o_sobre6ppm() {
        listOf(0.0, 1.0, 1.99).forEach {
            val r = ClasificadorCloro.clasificar(it)
            assertEquals(EstadoCloro.CRITICO, r.estado)
            assertEquals(TipoDesviacion.INSUFICIENTE, r.desviacion)
        }
        listOf(6.01, 7.0, 10.0).forEach {
            val r = ClasificadorCloro.clasificar(it)
            assertEquals(EstadoCloro.CRITICO, r.estado)
            assertEquals(TipoDesviacion.EXCESIVO, r.desviacion)
        }
    }

    @Test
    fun losRangosAnterioresYaNoSeConsideranNormales() {
        // Antes 1,5 ppm era "normal"; con los rangos del docente es crítico.
        assertEquals(EstadoCloro.CRITICO, estado(1.5))
    }

    @Test
    fun clasificaCadaCentesimaSinHuecos() {
        var centesimas = 0
        while (centesimas <= 1000) {
            ClasificadorCloro.clasificar(centesimas / 100.0)
            centesimas++
        }
    }

    @Test
    fun redondeaADosDecimales() {
        assertEquals(2.99, ClasificadorCloro.redondear(2.994), 0.0)
        assertEquals(EstadoCloro.NORMAL, estado(5.004))
    }

    @Test
    fun rangosPersonalizadosSeRespetan() {
        val rangos = RangosCloro(criticoMin = 1.0, normalMin = 2.0, normalMax = 3.0, criticoMax = 4.0)
        assertEquals(EstadoCloro.NORMAL, ClasificadorCloro.clasificar(2.5, rangos).estado)
        assertEquals(EstadoCloro.CRITICO, ClasificadorCloro.clasificar(4.5, rangos).estado)
    }

    @Test
    fun validaQueLosRangosVayanDeMenorAMayor() {
        assertTrue(RangosCloro().esValido())
        assertFalse(RangosCloro(criticoMin = 3.0, normalMin = 2.0).esValido())
    }

    @Test
    fun formatoChilenoConDosDecimales() {
        assertEquals("3,50 ppm", Formato.ppm(3.5))
        assertEquals(3.5, Formato.parsear("3,5")!!, 0.0)
        assertNull(Formato.parsear("abc"))
    }
}
