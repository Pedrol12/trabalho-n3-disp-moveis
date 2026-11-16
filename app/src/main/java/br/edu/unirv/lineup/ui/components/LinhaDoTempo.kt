package br.edu.unirv.lineup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.unirv.lineup.data.model.LineupRules
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.StatusNavio
import br.edu.unirv.lineup.data.model.TideTable
import br.edu.unirv.lineup.data.model.corDeHex
import br.edu.unirv.lineup.ui.theme.Paleta
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ModoArrasto { MOVER, BORDA_ESQUERDA, BORDA_DIREITA }

private data class PreviaArrasto(
    val atracacao: ShipSchedule,
    val inicio: Double,
    val fim: Double,
    val valida: Boolean
)

private const val DIAS = 30
private val LARGURA_DIA = 38.dp
private val ALTURA_TOTAL = 128.dp
private val ALTURA_CABECALHO = 20.dp
private val TOPO_BARRA = 58.dp
private val ALTURA_BARRA = 42.dp
private val ZONA_BORDA = 16.dp

/**
 * Timeline de 30 dias de um berço: dias de maré baixa sombreados, curva da maré ativa e barras
 * dos navios. Toque abre o diálogo; arrastar o corpo move; arrastar as bordas redimensiona.
 */
@Composable
fun LinhaDoTempoBerco(
    atracacoes: List<ShipSchedule>,
    mare: TideTable?,
    diasMareBaixa: Set<Int>,
    janelaValida: (ShipSchedule, Double, Double) -> Boolean,
    aoTocar: (ShipSchedule) -> Unit,
    aoMover: (ShipSchedule, Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val rolagem = rememberScrollState()
    val medidor = rememberTextMeasurer()
    var previa by remember { mutableStateOf<PreviaArrasto?>(null) }

    val atracacoesAtuais by rememberUpdatedState(atracacoes)
    val aoTocarAtual by rememberUpdatedState(aoTocar)
    val aoMoverAtual by rememberUpdatedState(aoMover)
    val validaAtual by rememberUpdatedState(janelaValida)

    // Pontos normalizados da curva: x em dias (0..30), y em 0..1 (1 = maré mais alta).
    val curva = remember(mare) {
        val medicoes = mare?.measurements.orEmpty()
        if (medicoes.isEmpty()) {
            emptyList()
        } else {
            val minimo = medicoes.minOf { it.heightMeters }
            val maximo = medicoes.maxOf { it.heightMeters }
            val faixa = (maximo - minimo).coerceAtLeast(0.01)
            medicoes.map { ((it.fractionalDay - 1.0).toFloat()) to ((it.heightMeters - minimo) / faixa).toFloat() }
        }
    }

    val corGrade = MaterialTheme.colorScheme.outline
    val corTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val corFundo = MaterialTheme.colorScheme.surface
    val estiloDia = remember(corTexto) { TextStyle(fontSize = 10.sp, color = corTexto, fontWeight = FontWeight.Medium) }
    val rotulosDia = remember(estiloDia) { (1..DIAS).map { medidor.measure(it.toString().padStart(2, '0'), estiloDia) } }
    val rotulosDiaBaixa = remember(estiloDia) {
        val estilo = estiloDia.copy(color = Paleta.Marinho, fontWeight = FontWeight.Bold)
        (1..DIAS).map { medidor.measure(it.toString().padStart(2, '0'), estilo) }
    }

    Box(modifier.horizontalScroll(rolagem)) {
        Canvas(
            Modifier
                .width(LARGURA_DIA * DIAS)
                .height(ALTURA_TOTAL)
                .pointerInput(Unit) {
                    detectTapGestures { toque ->
                        val larguraDia = LARGURA_DIA.toPx()
                        val alvo = encontrar(atracacoesAtuais, toque, larguraDia, TOPO_BARRA.toPx(), ALTURA_BARRA.toPx(), ZONA_BORDA.toPx())
                        if (alvo != null) aoTocarAtual(alvo.first)
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val larguraDia = LARGURA_DIA.toPx()
                        val toqueInicial = awaitFirstDown(requireUnconsumed = false)
                        val achado = encontrar(
                            atracacoesAtuais, toqueInicial.position, larguraDia,
                            TOPO_BARRA.toPx(), ALTURA_BARRA.toPx(), ZONA_BORDA.toPx()
                        ) ?: return@awaitEachGesture
                        val (atracacao, modo) = achado
                        val passou = awaitTouchSlopOrCancellation(toqueInicial.id) { mudanca, sobra ->
                            if (abs(sobra.x) >= abs(sobra.y)) mudanca.consume()
                        } ?: return@awaitEachGesture

                        val duracao = atracacao.endDay - atracacao.startDay
                        var ultimaPrevia: PreviaArrasto? = null
                        val terminou = drag(passou.id) { mudanca ->
                            mudanca.consume()
                            val deslocamento = ((mudanca.position.x - toqueInicial.position.x) / larguraDia).roundToInt()
                            val (novoIni, novoFim) = when (modo) {
                                ModoArrasto.MOVER -> {
                                    val ini = (atracacao.startDay + deslocamento)
                                        .coerceIn(LineupRules.DIA_MIN, LineupRules.DIA_MAX - duracao)
                                    ini to ini + duracao
                                }
                                ModoArrasto.BORDA_ESQUERDA -> {
                                    val ini = (atracacao.startDay + deslocamento)
                                        .coerceIn(LineupRules.DIA_MIN, atracacao.endDay - 1.0)
                                    ini to atracacao.endDay
                                }
                                ModoArrasto.BORDA_DIREITA -> {
                                    val fim = (atracacao.endDay + deslocamento)
                                        .coerceIn(atracacao.startDay + 1.0, LineupRules.DIA_MAX)
                                    atracacao.startDay to fim
                                }
                            }
                            val nova = PreviaArrasto(atracacao, novoIni, novoFim, validaAtual(atracacao, novoIni, novoFim))
                            ultimaPrevia = nova
                            previa = nova
                        }
                        val final = ultimaPrevia
                        if (terminou && final != null) {
                            aoMoverAtual(atracacao, final.inicio, final.fim)
                        }
                        previa = null
                    }
                }
        ) {
            val larguraDia = LARGURA_DIA.toPx()
            val altura = size.height
            val cabecalho = ALTURA_CABECALHO.toPx()

            drawRect(corFundo)

            // Dias de maré baixa sombreados
            for (dia in diasMareBaixa) {
                if (dia in 1..DIAS) {
                    drawRect(
                        Paleta.MareBaixa.copy(alpha = 0.55f),
                        topLeft = Offset((dia - 1) * larguraDia, 0f),
                        size = Size(larguraDia, altura)
                    )
                }
            }

            // Grade vertical e rótulos
            for (dia in 0..DIAS) {
                val x = dia * larguraDia
                drawLine(corGrade.copy(alpha = 0.6f), Offset(x, cabecalho), Offset(x, altura), strokeWidth = 1f)
            }
            drawLine(corGrade, Offset(0f, cabecalho), Offset(size.width, cabecalho), strokeWidth = 1f)
            for (dia in 1..DIAS) {
                val r = if (dia in diasMareBaixa) rotulosDiaBaixa[dia - 1] else rotulosDia[dia - 1]
                drawText(
                    r,
                    topLeft = Offset((dia - 1) * larguraDia + (larguraDia - r.size.width) / 2f, (cabecalho - r.size.height) / 2f)
                )
            }

            // Curva da maré
            if (curva.size > 1) {
                val topoCurva = cabecalho + 8.dp.toPx()
                val baseCurva = altura - 8.dp.toPx()
                val caminho = Path()
                curva.forEachIndexed { i, (x, y) ->
                    val px = x * larguraDia
                    val py = baseCurva - y * (baseCurva - topoCurva)
                    if (i == 0) caminho.moveTo(px, py) else caminho.lineTo(px, py)
                }
                drawPath(
                    caminho,
                    color = Paleta.Aco.copy(alpha = 0.75f),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Barras dos navios
            val topo = TOPO_BARRA.toPx()
            val alturaBarra = ALTURA_BARRA.toPx()
            for (atr in atracacoes) {
                val emArrasto = previa?.takeIf { it.atracacao.id == atr.id && atr.id != null }
                val ini = emArrasto?.inicio ?: atr.startDay
                val fim = emArrasto?.fim ?: atr.endDay
                val valida = emArrasto?.valida ?: true
                val x0 = ((ini - 0.5) * larguraDia).toFloat()
                val x1 = ((fim - 0.5) * larguraDia).toFloat()
                val cor = corDeHex(atr.stripeColorHex.ifBlank { StatusNavio.corHex(atr.status) })
                val corBarra = if (valida) cor else cor.copy(alpha = 0.45f)
                drawRoundRect(corBarra, Offset(x0, topo), Size(x1 - x0, alturaBarra), CornerRadius(6.dp.toPx()))
                if (!valida) {
                    drawRoundRect(
                        Paleta.Erro, Offset(x0, topo), Size(x1 - x0, alturaBarra), CornerRadius(6.dp.toPx()),
                        style = Stroke(2.dp.toPx())
                    )
                }
                // Alças das bordas
                val alca = Color.White.copy(alpha = 0.8f)
                val xa = 4.dp.toPx()
                val h1 = topo + alturaBarra * 0.3f
                val h2 = topo + alturaBarra * 0.7f
                drawLine(alca, Offset(x0 + xa, h1), Offset(x0 + xa, h2), 2.dp.toPx(), StrokeCap.Round)
                drawLine(alca, Offset(x1 - xa, h1), Offset(x1 - xa, h2), 2.dp.toPx(), StrokeCap.Round)

                val corTextoBarra = if (cor.luminance() > 0.45f) Color(0xFF10202E) else Color.White
                val larguraTexto = (x1 - x0 - 16.dp.toPx()).toInt().coerceAtLeast(1)
                val titulo = medidor.measure(
                    atr.shipName,
                    TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = corTextoBarra),
                    overflow = TextOverflow.Ellipsis, softWrap = false, maxLines = 1,
                    constraints = Constraints(maxWidth = larguraTexto)
                )
                val detalhe = medidor.measure(
                    "${LineupRules.formatarDia(ini)}-${LineupRules.formatarDia(fim)}  ${atr.status}",
                    TextStyle(fontSize = 10.sp, color = corTextoBarra.copy(alpha = 0.9f)),
                    overflow = TextOverflow.Clip, softWrap = false, maxLines = 1,
                    constraints = Constraints(maxWidth = larguraTexto)
                )
                drawText(titulo, topLeft = Offset(x0 + 8.dp.toPx(), topo + 5.dp.toPx()))
                drawText(detalhe, topLeft = Offset(x0 + 8.dp.toPx(), topo + 5.dp.toPx() + titulo.size.height))
            }
        }
    }
}

/** Localiza a barra sob o toque e o modo de arrasto conforme a zona (borda ou corpo). */
private fun encontrar(
    atracacoes: List<ShipSchedule>,
    ponto: Offset,
    larguraDia: Float,
    topo: Float,
    alturaBarra: Float,
    zonaBorda: Float
): Pair<ShipSchedule, ModoArrasto>? {
    if (ponto.y < topo - 8f || ponto.y > topo + alturaBarra + 8f) return null
    for (atr in atracacoes) {
        val x0 = ((atr.startDay - 0.5) * larguraDia).toFloat()
        val x1 = ((atr.endDay - 0.5) * larguraDia).toFloat()
        if (ponto.x < x0 || ponto.x > x1) continue
        val borda = minOf(zonaBorda, (x1 - x0) / 3f)
        val modo = when {
            ponto.x <= x0 + borda -> ModoArrasto.BORDA_ESQUERDA
            ponto.x >= x1 - borda -> ModoArrasto.BORDA_DIREITA
            else -> ModoArrasto.MOVER
        }
        return atr to modo
    }
    return null
}
