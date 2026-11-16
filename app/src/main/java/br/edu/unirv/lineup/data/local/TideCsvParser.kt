package br.edu.unirv.lineup.data.local

import android.content.Context
import br.edu.unirv.lineup.data.model.TideMeasurement
import br.edu.unirv.lineup.data.model.TideTable
import java.io.InputStream

/**
 * Lê os CSV de maré (formato Dia;Horario;Altura_m;Estado_Mare;Mes_Referencia), com 48 leituras
 * por dia (de 30 em 30 minutos). Segue a mesma regra do backend (PortService.parseAndSaveTideCsv).
 */
object TideCsvParser {

    const val PASTA_ASSETS = "tides"

    fun parse(texto: String): List<TideMeasurement> {
        val resultado = ArrayList<TideMeasurement>(1440)
        var cabecalhoPulado = false
        for (bruta in texto.lineSequence()) {
            val linha = bruta.trim()
            if (linha.isEmpty() || linha.startsWith("#")) continue
            val partes = if (linha.contains(";")) linha.split(";") else linha.split(",")
            if (!cabecalhoPulado) {
                val primeira = partes[0].lowercase()
                if (primeira.contains("dia") || primeira.contains("data")) {
                    cabecalhoPulado = true
                    continue
                }
            }
            if (partes.size < 3) continue
            val dia = partes[0].trim().toIntOrNull() ?: continue
            val horario = partes[1].trim()
            val altura = partes[2].trim().replace(",", ".").toDoubleOrNull() ?: continue
            val estado = if (partes.size >= 4) partes[3].trim().uppercase() else if (altura >= 1.0) "ALTA" else "BAIXA"
            val alta = estado.contains("ALTA") || estado.contains("HIGH") || (!estado.contains("BAIXA") && altura >= 1.0)
            val hm = horario.split(":")
            val hora = hm[0].toIntOrNull() ?: continue
            val minuto = if (hm.size > 1) hm[1].toIntOrNull() ?: 0 else 0
            val fracionario = dia + (hora * 60.0 + minuto) / 1440.0
            resultado.add(TideMeasurement(null, dia, horario, fracionario, altura, alta))
        }
        return resultado
    }

    fun parse(entrada: InputStream): List<TideMeasurement> =
        parse(entrada.bufferedReader(Charsets.UTF_8).use { it.readText() })

    /** "mare_novembro_2026.csv" -> "Novembro / 2026". */
    fun rotuloDoArquivo(nomeArquivo: String): String {
        val partes = nomeArquivo.removeSuffix(".csv").removePrefix("mare_").split("_")
        val mes = partes.firstOrNull().orEmpty().replaceFirstChar { it.uppercase() }
        val ano = partes.getOrNull(1).orEmpty()
        return if (ano.isEmpty()) mes else "$mes / $ano"
    }

    /**
     * Carrega todas as tábuas de assets/tides. Ids são negativos (locais). A tábua do mês
     * indicado em [mesAtivo] (ex.: "novembro") fica ativa; se não existir, a primeira.
     */
    fun carregarDosAssets(context: Context, mesAtivo: String = "novembro"): List<TideTable> {
        val arquivos = (context.assets.list(PASTA_ASSETS) ?: emptyArray())
            .filter { it.endsWith(".csv") }.sorted()
        val tabuas = arquivos.mapIndexed { indice, nome ->
            val medicoes = context.assets.open("$PASTA_ASSETS/$nome").use { parse(it) }
            val rotulo = rotuloDoArquivo(nome)
            TideTable(
                id = -(indice + 1).toLong(),
                monthKey = "$rotulo (30 Dias)",
                monthLabel = rotulo,
                sourceFileName = nome,
                activeMonth = nome.contains(mesAtivo, ignoreCase = true),
                measurements = medicoes
            )
        }
        return if (tabuas.isNotEmpty() && tabuas.none { it.activeMonth }) {
            tabuas.mapIndexed { i, t -> t.copy(activeMonth = i == 0) }
        } else tabuas
    }
}
