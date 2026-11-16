package br.edu.unirv.lineup

import br.edu.unirv.lineup.data.model.RailRules
import br.edu.unirv.lineup.data.model.WagonAllocation
import br.edu.unirv.lineup.ui.screens.rail.potenciaHp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RailRulesTest {

    @Test
    fun categoriaDeStatus_classificaTextosDoBackend() {
        assertEquals(RailRules.STATUS_CONCLUIDA, RailRules.categoriaDeStatus("Concluída / Escoada"))
        assertEquals(RailRules.STATUS_OCUPANDO, RailRules.categoriaDeStatus("Ocupando a Pera (Em Operação)"))
        assertEquals(RailRules.STATUS_APROXIMACAO, RailRules.categoriaDeStatus("Em Aproximação (Descendo a Serra)"))
        assertEquals(RailRules.STATUS_PROGRAMADA, RailRules.categoriaDeStatus("Programada"))
    }

    @Test
    fun posicao_formataELe() {
        assertEquals("Posição #007", RailRules.rotuloPosicao(7))
        assertEquals(7, RailRules.numeroDaPosicao("Posição #007"))
        assertEquals(0, RailRules.numeroDaPosicao("sem número"))
    }

    @Test
    fun proximaPosicao_usaMaiorDaLocomotiva() {
        fun a(loc: String, pos: String) = WagonAllocation(allocationId = pos + loc, locomotiveCode = loc, positionOrder = pos)
        val lista = listOf(a("L1", "Posição #001"), a("L1", "Posição #003"), a("L2", "Posição #009"))
        assertEquals("Posição #004", RailRules.proximaPosicao(lista, "L1"))
        assertEquals("Posição #001", RailRules.proximaPosicao(lista, "L3"))
    }

    @Test
    fun ordenarAlocacoes_porLocomotivaEPosicaoNumerica() {
        val l = listOf(
            WagonAllocation(allocationId = "3", locomotiveCode = "B", positionOrder = "Posição #001"),
            WagonAllocation(allocationId = "2", locomotiveCode = "A", positionOrder = "Posição #010"),
            WagonAllocation(allocationId = "1", locomotiveCode = "A", positionOrder = "Posição #002")
        )
        assertEquals(listOf("1", "2", "3"), RailRules.ordenarAlocacoes(l).map { it.allocationId })
    }

    @Test
    fun horaValida_limitaA0Ate23() {
        assertEquals(0, RailRules.horaValida(-1))
        assertEquals(23, RailRules.horaValida(24))
        assertEquals(10, RailRules.horaValida(10))
    }

    @Test
    fun anguloDoAnel_comecaNoOesteEGiraAntiHorario() {
        assertEquals(180.0, RailRules.anguloDoAnel(0.0), 1e-9)
        assertEquals(270.0, RailRules.anguloDoAnel(0.25), 1e-9)
        assertEquals(540.0, RailRules.anguloDoAnel(1.0), 1e-9)
    }

    @Test
    fun faixaHoraria_validaFormatoEOrdem() {
        assertTrue(RailRules.faixaValida("00:00", "04:00"))
        assertFalse(RailRules.faixaValida("12:00", "08:00"))
        assertFalse(RailRules.faixaValida("25:00", "26:00"))
        assertFalse(RailRules.horarioValido("8:00"))
    }

    @Test
    fun data_validaFormatoBrasileiro() {
        assertTrue(RailRules.dataValida("04/11/2026"))
        assertFalse(RailRules.dataValida("2026-11-04"))
        assertFalse(RailRules.dataValida("32/01/2026"))
    }

    @Test
    fun horaNaJanela_inicioInclusivoFimExclusivo() {
        assertTrue(RailRules.horaNaJanela(8, 8, 12))
        assertFalse(RailRules.horaNaJanela(12, 8, 12))
    }

    @Test
    fun potenciaHp_extraiDoModelo() {
        assertEquals("4.500 HP", potenciaHp("Wabtec ES44ACi • 4.500 HP"))
        assertNull(potenciaHp("Modelo sem potência"))
    }
}
