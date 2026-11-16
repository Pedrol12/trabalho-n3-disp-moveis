package br.edu.unirv.lineup.data.model

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** Status válidos de uma atracação e a cor da barra no Lineup. */
object StatusNavio {
    const val OPERANDO = "Operando"
    const val PRIORIDADE = "Prioridade"
    const val PROGRAMADO = "Programado"
    const val CONFIRMADO = "Confirmado"

    val todos = listOf(OPERANDO, PRIORIDADE, PROGRAMADO, CONFIRMADO)

    /** Mesma regra do backend (inferColorFromStatus). */
    fun corHex(status: String?): String {
        val s = status.orEmpty().lowercase()
        return when {
            s.contains("prioridade") || s.contains("operando") -> "#e74c3c"
            s.contains("confirmado") -> "#2ecc71"
            else -> "#e67e22"
        }
    }
}

/** Regras de negócio do Lineup validadas localmente antes de chamar o servidor. */
object LineupRules {
    const val DIA_MIN = 1.0
    const val DIA_MAX = 30.0

    /** RN002: primeiro dia de maré baixa dentro da janela, ou -1. Mesma conta do backend. */
    fun primeiroDiaMareBaixa(start: Double, end: Double, diasMareBaixa: Set<Int>): Int {
        if (diasMareBaixa.isEmpty()) return -1
        val primeiro = floor(start).toInt()
        val ultimo = max(primeiro.toDouble(), ceil(end - 0.01)).toInt()
        for (d in primeiro..ultimo) if (d in diasMareBaixa) return d
        return -1
    }

    /** Retorna a mensagem de erro (RN001/RN002/janela) ou null quando a janela é válida. */
    fun validarJanela(
        berth: Berth,
        start: Double,
        end: Double,
        ignoreScheduleId: Long?,
        diasMareBaixa: Set<Int>
    ): String? {
        if (start < DIA_MIN || end > DIA_MAX || end <= start) {
            return "Janela inválida: o período deve estar entre os dias 1 e 30 e a saída deve ser maior que a chegada."
        }
        val conflito = berth.findConflictingShip(start, end, ignoreScheduleId)
        if (conflito != null) {
            return "Colisão no berço ${berth.id} com o navio ${conflito.shipName} " +
                "(dias ${formatarDia(conflito.startDay)} a ${formatarDia(conflito.endDay)})."
        }
        val baixa = primeiroDiaMareBaixa(start, end, diasMareBaixa)
        if (baixa != -1) {
            return "Conflito de maré baixa no dia %02d: não é permitido atracar nem permanecer durante maré baixa.".format(baixa)
        }
        return null
    }

    /** RN004: duração sugerida em dias = ceil(toneladas / taxa diária), no mínimo 1. */
    fun duracaoSugerida(toneladas: Int, taxaDia: Int): Int {
        if (taxaDia <= 0) return 1
        return ceil(toneladas.toDouble() / taxaDia).toInt().coerceAtLeast(1)
    }

    /** Extrai o número de textos como "36.000 t/dia" -> 36000. */
    fun taxaPorDia(texto: String?): Int {
        val digitos = texto.orEmpty().takeWhile { !it.isLetter() }.filter { it.isDigit() }
        return digitos.toIntOrNull() ?: 0
    }

    fun formatarDia(valor: Double): String =
        if (valor == floor(valor)) valor.toInt().toString() else "%.1f".format(valor)
}

/** Taxa diária (t/dia) sugerida para uma atracação, a partir do catálogo de cargas. */
fun taxaSugerida(cargas: List<PortCargo>, categoria: CargoCategory, produtos: List<String>): Int {
    for (produto in produtos) {
        if (produto.isBlank()) continue
        val achada = cargas.firstOrNull { it.productName.equals(produto.trim(), ignoreCase = true) }
            ?: cargas.firstOrNull { it.productName.contains(produto.trim(), ignoreCase = true) }
        val taxa = LineupRules.taxaPorDia(achada?.averageRateTonsDay)
        if (taxa > 0) return taxa
    }
    val porPerfil = cargas.firstOrNull { it.cargoProfile == categoria }
    val taxa = LineupRules.taxaPorDia(porPerfil?.averageRateTonsDay)
    return if (taxa > 0) taxa else 15000
}
