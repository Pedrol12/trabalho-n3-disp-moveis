package br.edu.unirv.lineup.data.model

/*
 * Modelos do módulo Ferrovia. Os nomes dos campos são idênticos aos do JSON do LineupBackend
 * (RailDtos.java e entidades de domain/rail), conferidos com curl nos endpoints reais.
 */

data class TrainHourlySnapshot(
    val id: Long? = null,
    val prefix: String = "",
    val trunkAxis: String = "",
    val trunkAxisTitle: String = "",
    val originState: String = "",
    val destinationTerminal: String = "",
    val commodity: String = "",
    val wagonCount: Int = 0,
    val lengthKm: Double = 0.0,
    val tonnage: Int = 0,
    val scheduledDate: String = "",
    val arrivalHour: Int = 0,
    val departureHour: Int = 0,
    val durationHours: Int = 0,
    val scheduleWindowFormatted: String = "",
    val trainColorHex: String = "#3E6990",
    val occupyingPera: Boolean = false,
    val approaching: Boolean = false,
    val loopProgress: Double = -1.0,
    val loaded: Boolean = false,
    val statusAtHour: String = ""
) {
    val eixoMrs: Boolean get() = trunkAxis == "MRS_CREMALHEIRA"
}

data class PeraHourlyState(
    val date: String = "",
    val queryHour: Int = 0,
    val queryHourFormatted: String = "",
    val activeAmvBranches: Int = 0,
    val totalBranches: Int = 0,
    val occupyingTrain: TrainHourlySnapshot? = null,
    val approachingTrains: List<TrainHourlySnapshot> = emptyList(),
    val dailySchedule: List<TrainHourlySnapshot> = emptyList()
)

data class RailTerminalBranch(
    val code: String = "",
    val terminalName: String = "",
    val region: String = "",
    val hopperType: String = "",
    val mainCommodity: String = "",
    val capacityCarsPerDay: Int = 0,
    val switchActive: Boolean = true,
    val loopAnglePosition: Double = 0.0
)

data class RailAxisYard(
    val code: String = "",
    val name: String = "",
    val categoryType: String = "",
    val concessionaire: String = "",
    val maxTrainLengthKm: String = "",
    val switchAmvCount: Int = 0,
    val operationalStatus: String = ""
)

data class Wagon(
    val wagonCode: String = "",
    val product: String = "",
    val capacity: String = "",
    val originTerminal: String = "",
    val destinationTerminal: String = "",
    val locomotiveCode: String? = null
)

data class Locomotive(
    val locomotiveCode: String = "",
    val modelPower: String = "",
    val operatorAxis: String = "",
    val wagons: List<Wagon> = emptyList()
)

/** Corpo do POST/PUT /api/rail/locomotives (LocomotiveRequest). */
data class LocomotiveRequest(
    val locomotiveCode: String,
    val modelPower: String,
    val operatorAxis: String
)

/** Corpo do POST/PUT /api/rail/wagons (WagonRequest). */
data class WagonRequest(
    val wagonCode: String,
    val product: String,
    val capacity: String,
    val originTerminal: String,
    val destinationTerminal: String,
    val locomotiveCode: String?
)

data class WagonAllocation(
    val allocationId: String = "",
    val locomotiveCode: String = "",
    val positionOrder: String = "",
    val wagonCode: String = "",
    val product: String = "",
    val capacity: String = "",
    val originTerminal: String = "",
    val destinationTerminal: String = ""
)

data class LocomotiveArrival(
    val arrivalId: String = "",
    val locomotiveCode: String = "",
    val operatorAxis: String = "",
    val dateFormatted: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val product: String = "",
    val wagonSummary: String = "",
    val originTerminal: String = "",
    val destinationTerminal: String = "",
    val status: String = ""
)

/** Regras puras da Ferrovia (testáveis sem Android). */
object RailRules {

    const val STATUS_CONCLUIDA = "Concluída"
    const val STATUS_OCUPANDO = "Ocupando a Pera"
    const val STATUS_APROXIMACAO = "Em Aproximação"
    const val STATUS_PROGRAMADA = "Programada"

    /** Classifica o texto livre de status das chegadas em uma das 4 categorias. */
    fun categoriaDeStatus(status: String): String {
        val s = status.lowercase()
        return when {
            s.startsWith("conclu") -> STATUS_CONCLUIDA
            s.contains("ocupando") -> STATUS_OCUPANDO
            s.contains("aproxima") -> STATUS_APROXIMACAO
            else -> STATUS_PROGRAMADA
        }
    }

    /** Rótulo "Posição #007" a partir de um número. */
    fun rotuloPosicao(n: Int): String = "Posição #" + n.coerceAtLeast(0).toString().padStart(3, '0')

    /** Extrai o número de "Posição #003" (0 se não houver dígitos). */
    fun numeroDaPosicao(rotulo: String): Int = rotulo.filter(Char::isDigit).toIntOrNull() ?: 0

    /** Próxima posição livre na locomotiva (max + 1). */
    fun proximaPosicao(alocacoes: List<WagonAllocation>, locomotiva: String): String =
        rotuloPosicao((alocacoes.filter { it.locomotiveCode == locomotiva }.maxOfOrNull { numeroDaPosicao(it.positionOrder) } ?: 0) + 1)

    /** Ordena a alocação por locomotiva e depois pela posição sequencial. */
    fun ordenarAlocacoes(lista: List<WagonAllocation>): List<WagonAllocation> =
        lista.sortedWith(compareBy({ it.locomotiveCode }, { numeroDaPosicao(it.positionOrder) }))

    /** Hora de consulta sempre dentro de 0..23. */
    fun horaValida(hora: Int): Int = hora.coerceIn(0, 23)

    /** Posição (0..1) no anel da Pera -> ângulo em graus, sentido anti-horário a partir do oeste. */
    fun anguloDoAnel(posicao: Double): Double = 180.0 + 360.0 * posicao.coerceIn(0.0, 1.0)

    /** Verdadeiro se [hora] está dentro da janela [inicio, fim). */
    fun horaNaJanela(hora: Int, inicio: Int, fim: Int): Boolean = hora >= inicio && hora < fim

    private val padraoHorario = Regex("""([01]\d|2[0-3]):[0-5]\d""")
    private val padraoData = Regex("""(0[1-9]|[12]\d|3[01])/(0[1-9]|1[0-2])/\d{4}""")

    fun horarioValido(texto: String): Boolean = padraoHorario.matches(texto.trim())

    fun dataValida(texto: String): Boolean = padraoData.matches(texto.trim())

    private fun minutos(texto: String): Int {
        val (h, m) = texto.trim().split(":")
        return h.toInt() * 60 + m.toInt()
    }

    /** Faixa horária HH:mm válida e com início anterior ao fim. */
    fun faixaValida(inicio: String, fim: String): Boolean =
        horarioValido(inicio) && horarioValido(fim) && minutos(inicio) < minutos(fim)
}
