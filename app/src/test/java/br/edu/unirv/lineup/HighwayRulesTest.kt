package br.edu.unirv.lineup

import br.edu.unirv.lineup.data.model.HighwayLine
import br.edu.unirv.lineup.data.model.HighwayRules
import br.edu.unirv.lineup.data.model.TruckSchedule
import br.edu.unirv.lineup.ui.screens.highway.slotDoToque
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighwayRulesTest {

    private fun doca() = HighwayLine(
        lineNumber = 1, lineCode = "LINHA-001 (Doca 001)",
        trucks = listOf(
            TruckSchedule(truckId = "A", plateAndFleet = "KLM-7N45 • Frota #108", startSlot = 8, endSlot = 20),
            TruckSchedule(truckId = "B", plateAndFleet = "PQR-2S61 • Frota #109", startSlot = 30, endSlot = 40)
        )
    )

    @Test
    fun hasOverlap_detectaColisaoNoMesmoSlot() {
        assertTrue(doca().hasOverlap(19, 25))
        assertTrue(doca().hasOverlap(0, 9))
        assertTrue(doca().hasOverlap(10, 12))
    }

    @Test
    fun hasOverlap_extremosEncostadosNaoColidem() {
        assertFalse(doca().hasOverlap(20, 30))
        assertFalse(doca().hasOverlap(0, 8))
        assertFalse(doca().hasOverlap(40, 50))
    }

    @Test
    fun hasOverlap_ignoraOProprioAgendamento() {
        assertTrue(doca().hasOverlap(8, 20))
        assertFalse(doca().hasOverlap(8, 20, "A"))
        assertTrue(doca().hasOverlap(15, 35, "A"))
    }

    @Test
    fun validarJanela_rejeitaForaDoMesEInvertida() {
        assertNotNull(HighwayRules.validarJanela(doca(), 50, 50, null))
        assertNotNull(HighwayRules.validarJanela(doca(), 60, 55, null))
        assertNotNull(HighwayRules.validarJanela(doca(), -1, 5, null))
        assertNotNull(HighwayRules.validarJanela(doca(), 2800, 2881, null))
        assertNull(HighwayRules.validarJanela(doca(), 2868, 2880, null))
        assertNull(HighwayRules.validarJanela(doca(), 50, 60, null))
    }

    @Test
    fun validarJanela_mensagemDeColisao() {
        val erro = HighwayRules.validarJanela(doca(), 10, 15, null)
        assertTrue(erro!!.contains("KLM-7N45"))
    }

    @Test
    fun alinharMinuto_arredondaParaQuartoDeHora() {
        assertEquals(0, HighwayRules.alinharMinuto(14))
        assertEquals(15, HighwayRules.alinharMinuto(15))
        assertEquals(30, HighwayRules.alinharMinuto(44))
        assertEquals(45, HighwayRules.alinharMinuto(59))
    }

    @Test
    fun slotDe_batecomOBackend() {
        assertEquals(0, HighwayRules.slotDe(1, 0, 0))
        assertEquals(8, HighwayRules.slotDe(1, 2, 0))
        assertEquals(218, HighwayRules.slotDe(3, 6, 30))
        assertEquals(2879, HighwayRules.slotDe(30, 23, 45))
        assertEquals(97, HighwayRules.slotDe(2, 0, 20)) // 20 min alinha para 15
    }

    @Test
    fun slotsRoundTrip_diaHoraMinuto() {
        for (slot in listOf(0, 8, 75, 218, 259, 2879)) {
            val refeito = HighwayRules.slotDe(
                HighwayRules.diaDoSlot(slot), HighwayRules.horaDoSlot(slot), HighwayRules.minutoDoSlot(slot)
            )
            assertEquals(slot, refeito)
        }
        assertEquals("Dia 03 • 06:30", HighwayRules.formatarSlot(218))
        assertEquals("Dia 30 • 24:00", HighwayRules.formatarSlot(2880))
    }

    @Test
    fun slotDoToque_mapeiaXParaSlotDoDia() {
        assertEquals(0, slotDoToque(0f, 10f, 1))
        assertEquals(5, slotDoToque(55f, 10f, 1))
        assertEquals(95, slotDoToque(5000f, 10f, 1))
        assertEquals(96 + 3, slotDoToque(35f, 10f, 2))
    }

    @Test
    fun placaEFrota_montaESepara() {
        assertEquals("KLM-7N45 • Frota #108", HighwayRules.montarPlacaFrota("klm-7n45", "108"))
        assertEquals("KLM-7N45" to "108", HighwayRules.separarPlacaFrota("KLM-7N45 • Frota #108"))
        assertTrue(HighwayRules.placaValida("KLM-7N45"))
        assertTrue(HighwayRules.placaValida("klm7n45"))
        assertFalse(HighwayRules.placaValida("12-ABC"))
        assertEquals("KLM-7N45", HighwayRules.normalizarPlaca("klm7n45"))
    }

    @Test
    fun permanencia_formataELe() {
        assertEquals("03h15min (13 slots de 15m)", HighwayRules.formatarPermanencia(13))
        assertEquals("00h15min (1 slot de 15m)", HighwayRules.formatarPermanencia(1))
        assertEquals(13, HighwayRules.slotsDaPermanencia("03h15min (13 slots de 15m)"))
        assertEquals(1, HighwayRules.slotsDaPermanencia("00h15min (1 slot de 15m)"))
        assertNull(HighwayRules.slotsDaPermanencia("sem formato"))
    }
}
