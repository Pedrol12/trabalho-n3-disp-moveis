package br.edu.unirv.lineup.data.model

/*
 * Modelos do módulo Portos. Os nomes dos campos são idênticos aos do JSON do LineupBackend
 * (PortDtos.java e entidades JPA). Todos têm valor padrão para o Gson conseguir instanciar.
 */

data class ShipSchedule(
    val id: Long? = null,
    val berthId: String = "",
    val berthName: String = "",
    val shipName: String = "",
    val unloadingProduct: String = "",
    val loadingProduct: String = "",
    val tonnage: Int = 0,
    val startDay: Double = 1.0,
    val endDay: Double = 2.0,
    val stripeColorHex: String = "",
    val status: String = "Programado",
    val cargoTypeSummary: String = "",
    val durationDays: Double = 0.0
)

data class Berth(
    val id: String = "",
    val name: String = "",
    val sectorName: String = "",
    val maxDraft: String = "",
    val category: CargoCategory = CargoCategory.MULTIPURPOSE,
    val displayOrder: Int = 0,
    val schedules: List<ShipSchedule> = emptyList()
) {
    /** RN001: existe navio do berço colidindo com a janela candidata? */
    fun hasOverlap(candidateStart: Double, candidateEnd: Double, ignoreScheduleId: Long?): Boolean =
        findConflictingShip(candidateStart, candidateEnd, ignoreScheduleId) != null

    fun findConflictingShip(candidateStart: Double, candidateEnd: Double, ignoreScheduleId: Long?): ShipSchedule? =
        schedules.firstOrNull { outro ->
            val ignorar = ignoreScheduleId != null && outro.id == ignoreScheduleId
            !ignorar && candidateStart < outro.endDay && candidateEnd > outro.startDay
        }

    /** Primeira janela livre (inteira) respeitando colisão e maré baixa, tentando a duração preferida e reduzindo. */
    fun primeiraJanelaLivre(duracaoPreferida: Int, diasMareBaixa: Set<Int>): Pair<Double, Double>? {
        val preferida = duracaoPreferida.coerceIn(1, 29)
        for (tamanho in preferida downTo 1) {
            for (inicio in 1..(30 - tamanho)) {
                val fim = (inicio + tamanho).toDouble()
                if (LineupRules.validarJanela(this, inicio.toDouble(), fim, null, diasMareBaixa) == null) {
                    return inicio.toDouble() to fim
                }
            }
        }
        return null
    }
}

data class RegisteredVessel(
    val imoCode: String = "",
    val shipName: String = "",
    val vesselClass: String = "",
    val maxDraftMeters: String = "",
    val defaultTonnage: Int = 0,
    val defaultUnloadProduct: String = "",
    val defaultLoadProduct: String = ""
)

data class PortCargo(
    val productCode: String = "",
    val productName: String = "",
    val operationDirection: String = "",
    val cargoProfile: CargoCategory = CargoCategory.MULTIPURPOSE,
    val averageRateTonsDay: String = ""
)

data class TideMeasurement(
    val id: Long? = null,
    val dayNumber: Int = 1,
    val timeStr: String = "00:00",
    val fractionalDay: Double = 1.0,
    val heightMeters: Double = 0.0,
    val highTide: Boolean = true
)

data class TideTable(
    val id: Long? = null,
    val monthKey: String = "",
    val monthLabel: String = "",
    val sourceFileName: String = "",
    val activeMonth: Boolean = false,
    val measurements: List<TideMeasurement> = emptyList()
) {
    /** Dias com ao menos uma leitura de maré baixa (bloqueados para atracação). */
    fun diasMareBaixa(): Set<Int> =
        measurements.filter { !it.highTide }.map { it.dayNumber }.toSortedSet()

    fun resumoDoDia(dia: Int): ResumoDiaMare? {
        val doDia = measurements.filter { it.dayNumber == dia }
        if (doDia.isEmpty()) return null
        return ResumoDiaMare(
            dia = dia,
            minimo = doDia.minOf { it.heightMeters },
            maximo = doDia.maxOf { it.heightMeters },
            liberado = doDia.all { it.highTide }
        )
    }
}

data class ResumoDiaMare(val dia: Int, val minimo: Double, val maximo: Double, val liberado: Boolean)

data class TideTableSummary(
    val id: Long? = null,
    val monthKey: String = "",
    val monthLabel: String = "",
    val sourceFileName: String = "",
    val activeMonth: Boolean = false,
    val totalMeasurements: Int = 0,
    val lowTideDays: Set<Int> = emptySet()
)

// ---- Requisições (BerthRequest, ShipScheduleRequest, ShipScheduleWindowUpdateRequest) ----

data class BerthRequest(
    val id: String,
    val name: String,
    val sectorName: String?,
    val maxDraft: String?,
    val category: CargoCategory
)

data class ShipScheduleRequest(
    val berthId: String,
    val shipName: String,
    val unloadingProduct: String?,
    val loadingProduct: String?,
    val tonnage: Int,
    val startDay: Double,
    val endDay: Double,
    val stripeColorHex: String?,
    val status: String?
)

data class ShipScheduleWindowUpdateRequest(
    val startDay: Double,
    val endDay: Double,
    val targetBerthId: String?
)
