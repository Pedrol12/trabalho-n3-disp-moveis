package br.edu.unirv.lineup

import br.edu.unirv.lineup.data.local.TideCsvParser
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.LineupRules
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.TideTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LineupRulesTest {

    private val berco = Berth(
        id = "SV-01",
        name = "Berço 1",
        schedules = listOf(
            ShipSchedule(id = 1, berthId = "SV-01", shipName = "A", startDay = 2.0, endDay = 5.0),
            ShipSchedule(id = 2, berthId = "SV-01", shipName = "B", startDay = 8.0, endDay = 10.0)
        )
    )

    // ---- RN001: colisão ----

    @Test
    fun hasOverlap_detectaSobreposicao() {
        assertTrue(berco.hasOverlap(4.0, 6.0, null))
        assertTrue(berco.hasOverlap(1.0, 3.0, null))
        assertTrue(berco.hasOverlap(3.0, 4.0, null))
    }

    @Test
    fun hasOverlap_extremosEncostadosNaoColidem() {
        assertFalse(berco.hasOverlap(5.0, 8.0, null))
        assertFalse(berco.hasOverlap(1.0, 2.0, null))
        assertFalse(berco.hasOverlap(10.0, 12.0, null))
    }

    @Test
    fun hasOverlap_ignoraAPropriaAtracacao() {
        assertTrue(berco.hasOverlap(2.0, 5.0, null))
        assertFalse(berco.hasOverlap(2.0, 6.0, 1L))
        assertTrue(berco.hasOverlap(2.0, 9.0, 1L))
    }

    // ---- RN002: maré baixa ----

    @Test
    fun mareBaixa_bloqueiaJanelaQueContemDiaBloqueado() {
        val baixa = setOf(15, 16)
        assertEquals(15, LineupRules.primeiroDiaMareBaixa(14.0, 17.0, baixa))
        assertEquals(16, LineupRules.primeiroDiaMareBaixa(16.0, 18.0, baixa))
        assertEquals(-1, LineupRules.primeiroDiaMareBaixa(17.0, 20.0, baixa))
        assertEquals(-1, LineupRules.primeiroDiaMareBaixa(12.0, 15.0, setOf(16)))
    }

    @Test
    fun validarJanela_rejeitaMareBaixaColisaoEJanelaInvalida() {
        val baixa = setOf(20)
        assertNull(LineupRules.validarJanela(berco, 12.0, 15.0, null, baixa))
        assertNotNull(LineupRules.validarJanela(berco, 18.0, 22.0, null, baixa))
        assertNotNull(LineupRules.validarJanela(berco, 4.0, 6.0, null, baixa))
        assertNotNull(LineupRules.validarJanela(berco, 0.0, 3.0, 99L, emptySet()))
        assertNotNull(LineupRules.validarJanela(berco, 20.0, 31.0, null, emptySet()))
        assertNotNull(LineupRules.validarJanela(berco, 12.0, 12.0, null, emptySet()))
    }

    // ---- RN004 ----

    @Test
    fun duracaoSugerida_usaTetoDaDivisao() {
        assertEquals(2, LineupRules.duracaoSugerida(50000, 36000))
        assertEquals(1, LineupRules.duracaoSugerida(1000, 36000))
        assertEquals(3, LineupRules.duracaoSugerida(60000, 20000))
        assertEquals(36000, LineupRules.taxaPorDia("36.000 t/dia"))
        assertEquals(2800, LineupRules.taxaPorDia("2.800 mov/dia"))
    }

    // ---- CSV de maré ----

    @Test
    fun tideCsvParser_leQuarentaEOitoLeiturasPorDia() {
        val arquivo = File("src/main/assets/tides/mare_novembro_2026.csv")
        val medicoes = TideCsvParser.parse(arquivo.readText())
        assertEquals(30 * 48, medicoes.size)
        assertEquals(48, medicoes.count { it.dayNumber == 7 })
        val tabua = TideTable(measurements = medicoes)
        assertEquals(setOf(11, 12, 15, 16, 23, 24, 30), tabua.diasMareBaixa())
    }
}
