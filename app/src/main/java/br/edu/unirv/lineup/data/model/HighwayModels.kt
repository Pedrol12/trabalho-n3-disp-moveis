package br.edu.unirv.lineup.data.model

/*
 * Modelos do módulo Rodovia. Os nomes dos campos são idênticos aos do JSON do LineupBackend
 * (HighwayDtos.java e entidades de domain/highway), conferidos com curl nos endpoints reais.
 */

/** Agendamento de caminhão numa doca (JSON de TruckScheduleEntity). Slots de 15 min: 0..2880. */
data class TruckSchedule(
    val truckId: String = "",
    val plateAndFleet: String = "",
    val carrierName: String = "",
    val unloadingProduct: String = "",
    val loadingProduct: String = "",
    val tonnage: Double = 0.0,
    val startSlot: Int = 0,
    val endSlot: Int = 1,
    val colorHex: String = "",
    val durationSlots: Int = 1,
    val lineCode: String = "",
    val lineNumber: Int = 0,
    val windowFormatted: String = ""
)

/** Doca/linha com seus agendamentos (HighwayLineWithTrucksDto). */
data class HighwayLine(
    val lineNumber: Int = 0,
    val lineCode: String = "",
    val sectorName: String = "",
    val cargoProfile: String = "",
    val sectorColorHex: String = "",
    val truckCount: Int = 0,
    val trucks: List<TruckSchedule> = emptyList()
) {
    /** RN017: true se [inicio, fim) colide com algum caminhão da doca (exceto [ignorarTruckId]). */
    fun hasOverlap(inicio: Int, fim: Int, ignorarTruckId: String? = null): Boolean =
        conflitoCom(inicio, fim, ignorarTruckId) != null

    fun conflitoCom(inicio: Int, fim: Int, ignorarTruckId: String? = null): TruckSchedule? =
        trucks.firstOrNull { it.truckId != ignorarTruckId && inicio < it.endSlot && fim > it.startSlot }
}

/** Corpo de POST/PUT /api/highway/schedules (TruckScheduleRequest). Campos nulos são omitidos pelo Gson. */
data class TruckScheduleRequest(
    val truckId: String? = null,
    val lineNumber: Int,
    val plateAndFleet: String,
    val carrierName: String,
    val unloadingProduct: String,
    val loadingProduct: String,
    val tonnage: Double,
    val startSlot: Int,
    val endSlot: Int,
    val colorHex: String? = null
)

data class HighwaySector(
    val sectorCode: String = "",
    val sectorName: String = "",
    val accessHighway: String = "",
    val vehicleProfile: String = "",
    val gateCount: Int = 0,
    val colorHex: String = "",
    val operationalStatus: String = ""
)

/** Caminhão cadastrado (RegisteredTruckEntity). */
data class RegisteredTruck(
    val truckCode: String = "",
    val plateAndFleet: String = "",
    val carrierName: String = "",
    val truckConfigType: String = "",
    val maxCapacityTons: Double = 0.0,
    val defaultUnloadProduct: String = "",
    val defaultLoadProduct: String = ""
)

/** Carga rodoviária (HighwayCargoEntity). */
data class HighwayCargo(
    val cargoCode: String = "",
    val productName: String = "",
    val operationDirection: String = "",
    val equipmentType: String = "",
    val defaultStayDuration: String = ""
) {
    /** Retorno = "⬆ Carga / Retorno Interior"; qualquer outro texto é tratado como descarga. */
    val ehRetorno: Boolean get() = operationDirection.contains("⬆") || operationDirection.contains("Retorno", true)
}

/** Regras de tempo e validação do Gantt rodoviário (RN016 e RN017). */
object HighwayRules {
    const val DIAS = 30
    const val SLOTS_POR_DIA = 96
    const val TOTAL_SLOTS = DIAS * SLOTS_POR_DIA // 2880
    const val SLOTS_POR_HORA = 4
    const val DOCAS = 180
    const val PREFIXO_ID_TEMPORARIO = "TMP-"

    /** RN016: alinha minutos ao quarto de hora anterior (0, 15, 30, 45). */
    fun alinharMinuto(minuto: Int): Int = (minuto.coerceIn(0, 59) / 15) * 15

    /** Mesma conta de HighwayTimeSlots.toSlotIndex do backend (dia 1..30, hora 0..23). */
    fun slotDe(dia: Int, hora: Int, minuto: Int): Int =
        (dia.coerceIn(1, DIAS) - 1) * SLOTS_POR_DIA + hora.coerceIn(0, 23) * SLOTS_POR_HORA +
            alinharMinuto(minuto) / 15

    fun diaDoSlot(slot: Int): Int = slot.coerceIn(0, TOTAL_SLOTS - 1) / SLOTS_POR_DIA + 1
    fun horaDoSlot(slot: Int): Int = (slot.coerceIn(0, TOTAL_SLOTS - 1) % SLOTS_POR_DIA) / SLOTS_POR_HORA
    fun minutoDoSlot(slot: Int): Int = (slot.coerceIn(0, TOTAL_SLOTS - 1) % SLOTS_POR_HORA) * 15

    /** "Dia 03 • 06:30"; o slot 2880 vira "Dia 30 • 24:00" (igual ao backend). */
    fun formatarSlot(slot: Int): String {
        if (slot >= TOTAL_SLOTS) return "Dia 30 • 24:00"
        return "Dia %02d • %02d:%02d".format(diaDoSlot(slot), horaDoSlot(slot), minutoDoSlot(slot))
    }

    /** Só o horário "HH:mm" dentro do dia (24:00 para o fim do dia 30). */
    fun horaFormatada(slot: Int): String {
        if (slot >= TOTAL_SLOTS) return "24:00"
        return "%02d:%02d".format(horaDoSlot(slot), minutoDoSlot(slot))
    }

    /**
     * Valida a janela [inicio, fim) de um agendamento na [doca]. Devolve a mensagem de erro, ou null se válida.
     * Espelha HighwayService.validateTruckWindow (0 <= inicio < fim <= 2880 e sem colisão na doca).
     */
    fun validarJanela(doca: HighwayLine, inicio: Int, fim: Int, ignorarTruckId: String?): String? {
        if (inicio < 0 || fim > TOTAL_SLOTS || fim <= inicio) {
            return "Janela inválida: a saída deve ser posterior à chegada, dentro dos 30 dias."
        }
        val conflito = doca.conflitoCom(inicio, fim, ignorarTruckId)
        if (conflito != null) {
            return "Colisão na ${doca.lineCode} com o caminhão ${conflito.plateAndFleet} " +
                "(${formatarSlot(conflito.startSlot)} → ${formatarSlot(conflito.endSlot)})."
        }
        return null
    }

    /** Junta placa e frota no formato do backend: "KLM-7N45 • Frota #108". */
    fun montarPlacaFrota(placa: String, frota: String): String {
        val f = frota.trim().removePrefix("#").removePrefix("Frota").trim().removePrefix("#")
        return "${placa.trim().uppercase()} • Frota #$f"
    }

    /** Separa "KLM-7N45 • Frota #108" em ("KLM-7N45", "108"). Sem separador, a frota vem vazia. */
    fun separarPlacaFrota(texto: String): Pair<String, String> {
        val partes = texto.split("•", limit = 2)
        val placa = partes[0].trim()
        val frota = partes.getOrNull(1)?.trim().orEmpty().removePrefix("Frota").trim().removePrefix("#")
        return placa to frota
    }

    /** Formato do backend para permanência: 13 slots viram "03h15min (13 slots de 15m)". */
    fun formatarPermanencia(slots: Int): String {
        val n = slots.coerceAtLeast(1)
        val minutos = n * 15
        return "%02dh%02dmin (%d %s de 15m)".format(minutos / 60, minutos % 60, n, if (n == 1) "slot" else "slots")
    }

    private val SLOTS_NO_TEXTO = Regex("""\((\d+)\s+slots?""")

    /** Extrai o número de slots de "03h15min (13 slots de 15m)"; null se o texto não seguir o formato. */
    fun slotsDaPermanencia(texto: String): Int? = SLOTS_NO_TEXTO.find(texto)?.groupValues?.get(1)?.toIntOrNull()

    private val PLACA_MERCOSUL = Regex("^[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}$")

    /** Placa Mercosul (AAA0A00) ou antiga (AAA0000), com ou sem hífen. */
    fun placaValida(placa: String): Boolean = PLACA_MERCOSUL.matches(placa.trim().uppercase())

    fun normalizarPlaca(placa: String): String {
        val p = placa.trim().uppercase().replace("-", "")
        return if (p.length == 7) p.substring(0, 3) + "-" + p.substring(3) else p
    }
}
