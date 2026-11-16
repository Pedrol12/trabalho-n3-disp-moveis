package br.edu.unirv.lineup.ui.screens.rail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.unirv.lineup.data.model.PeraHourlyState
import br.edu.unirv.lineup.data.model.RailAxisYard
import br.edu.unirv.lineup.data.model.RailRules
import br.edu.unirv.lineup.data.model.RailTerminalBranch
import br.edu.unirv.lineup.data.model.TrainHourlySnapshot
import br.edu.unirv.lineup.ui.components.CartaoKpi
import br.edu.unirv.lineup.ui.components.EstadoVazio
import br.edu.unirv.lineup.ui.components.Etiqueta
import br.edu.unirv.lineup.ui.theme.Paleta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

/** RF010: estado da Pera ferroviária por data e hora, com Canvas topológico do anel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RailPeraScreen(
    estado: PeraHourlyState?,
    data: String,
    hora: Int,
    trens: List<TrainHourlySnapshot>,
    ramais: List<RailTerminalBranch>,
    eixos: List<RailAxisYard>,
    aoMudarHora: (Int) -> Unit,
    aoMudarData: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var seletorAberto by rememberSaveable { mutableStateOf(false) }
    val dataExibida = runCatching { LocalDate.parse(data).format(FORMATO_DATA) }.getOrDefault(data)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Data da consulta", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dataExibida, style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(onClick = { seletorAberto = true }) { Text("Escolher data") }
                    }
                    Text(
                        "Hora: %02d:00".format(hora),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Slider(
                        value = hora.toFloat(),
                        onValueChange = { aoMudarHora(it.toInt()) },
                        valueRange = 0f..23f,
                        steps = 22
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("00h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("23h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { aoMudarHora(hora - 1) }, enabled = hora > 0, modifier = Modifier.weight(1f)) { Text("-1h") }
                        OutlinedButton(onClick = { aoMudarHora(hora + 1) }, enabled = hora < 23, modifier = Modifier.weight(1f)) { Text("+1h") }
                    }
                }
            }
        }

        if (estado == null) {
            item { EstadoVazio("Sem dados da Pera. Conecte ao backend e atualize.") }
        } else {
            item {
                val maior = trens.maxOfOrNull { it.lengthKm } ?: 0.0
                val amvMalha = eixos.sumOf { it.switchAmvCount }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CartaoKpi(
                            "Extensão da composição",
                            "%.1f km".format(Locale.forLanguageTag("pt-BR"), maior),
                            Modifier.weight(1f)
                        )
                        CartaoKpi("AMVs na malha", amvMalha.toString(), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CartaoKpi("Moegas com AMV ativo", "${estado.activeAmvBranches}/${estado.totalBranches}", Modifier.weight(1f))
                        CartaoKpi("Trens no dia", trens.size.toString(), Modifier.weight(1f))
                    }
                }
            }
            item { TopologiaDaPera(estado, ramais) }
            item { LegendaDosRamais(ramais) }
            item {
                Text("Trens do dia", style = MaterialTheme.typography.titleMedium)
            }
            if (trens.isEmpty()) item { EstadoVazio("Nenhum trem programado para esta data.") }
            items(trens, key = { it.id ?: it.prefix.hashCode().toLong() }) { t -> CartaoTrem(t, hora) }
        }
    }

    if (seletorAberto) {
        val inicial = runCatching { LocalDate.parse(data) }.getOrDefault(LocalDate.now())
        val estadoSeletor = rememberDatePickerState(
            initialSelectedDateMillis = inicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { seletorAberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoSeletor.selectedDateMillis?.let { ms ->
                        aoMudarData(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    seletorAberto = false
                }) { Text("Aplicar") }
            },
            dismissButton = { TextButton(onClick = { seletorAberto = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estadoSeletor) }
    }
}

@Composable
private fun CartaoTrem(t: TrainHourlySnapshot, hora: Int) {
    val cor = corDeHex(t.trainColorHex)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (t.occupyingPera) 2.dp else 1.dp, if (t.occupyingPera) cor else MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.prefix, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(t.scheduleWindowFormatted, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(t.trunkAxisTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${t.commodity}  |  ${t.wagonCount} vagões  |  ${"%,d".format(Locale.forLanguageTag("pt-BR"), t.tonnage)} t  |  ${"%.2f".format(Locale.forLanguageTag("pt-BR"), t.lengthKm)} km", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            Text("${t.originState} → ${t.destinationTerminal}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            val agora = RailRules.horaNaJanela(hora, t.arrivalHour, t.departureHour)
            Etiqueta(
                t.statusAtHour,
                when {
                    t.occupyingPera || agora -> Paleta.Erro
                    t.approaching -> Paleta.Offline
                    hora >= t.departureHour -> Paleta.Online
                    else -> Paleta.Aco
                },
                Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun LegendaDosRamais(ramais: List<RailTerminalBranch>) {
    if (ramais.isEmpty()) return
    Column {
        Text("Moegas no anel (sentido anti-horário)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ramais.sortedBy { it.loopAnglePosition }.forEachIndexed { i, r ->
            Text(
                "${i + 1}  ${r.code}  |  ${if (r.switchActive) "AMV ativo" else "AMV desativado"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (r.switchActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Canvas topológico (não geográfico): anel unidirecional da Pera (anti-horário), os dois eixos de descida
 * chegando pelo oeste, as moegas posicionadas por loopAnglePosition e as composições conforme a hora.
 */
@Composable
private fun TopologiaDaPera(estado: PeraHourlyState, ramais: List<RailTerminalBranch>) {
    val medidor = rememberTextMeasurer()
    val corVia = Paleta.Aco
    val corTexto = Paleta.Texto
    val corSuave = Paleta.TextoSuave
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.15f).padding(8.dp)) {
            val w = size.width
            val h = size.height
            val cx = w * 0.62f
            val cy = h * 0.5f
            val rx = w * 0.30f
            val ry = h * 0.34f
            val junta = Offset(cx - rx, cy)
            val inicioMrs = Offset(w * 0.02f, h * 0.10f)
            val inicioRumo = Offset(w * 0.02f, h * 0.90f)
            val espessura = 5.dp.toPx()

            // eixos de descida
            drawLine(corVia, inicioMrs, junta, strokeWidth = espessura, cap = StrokeCap.Round)
            drawLine(corVia, inicioRumo, junta, strokeWidth = espessura, cap = StrokeCap.Round)
            rotulo(medidor, "MRS Cremalheira", Offset(inicioMrs.x, inicioMrs.y - 18.dp.toPx()), corSuave)
            rotulo(medidor, "Rumo Mairinque", Offset(inicioRumo.x, inicioRumo.y + 4.dp.toPx()), corSuave)

            // anel
            drawOval(corVia, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), style = Stroke(espessura))

            // setas do sentido anti-horário
            listOf(0.125, 0.375, 0.625, 0.875).forEach { p -> seta(cx, cy, rx, ry, p, corTexto) }

            // composição ocupando o anel
            estado.occupyingTrain?.takeIf { it.loopProgress in 0.0..1.0 }?.let { t ->
                val cor = corDeHex(t.trainColorHex)
                val anguloCabeca = RailRules.anguloDoAnel(t.loopProgress)
                drawArc(
                    cor, startAngle = (-anguloCabeca).toFloat(), sweepAngle = 40f, useCenter = false,
                    topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2),
                    style = Stroke(espessura * 2.2f, cap = StrokeCap.Round)
                )
                val pos = pontoNoAnel(cx, cy, rx, ry, t.loopProgress)
                drawCircle(Color.White, 8.dp.toPx(), pos)
                drawCircle(cor, 6.dp.toPx(), pos)
                rotulo(medidor, t.prefix, Offset(pos.x - 30.dp.toPx(), pos.y - 24.dp.toPx()), corTexto, negrito = true)
            }

            // composições em aproximação, sobre o eixo de origem
            estado.approachingTrains.forEach { t ->
                val inicio = if (t.eixoMrs) inicioMrs else inicioRumo
                val restante = (t.arrivalHour - estado.queryHour).coerceAtLeast(1)
                val fracao = 0.12f + 0.80f * (restante.coerceAtMost(6) / 6f)
                val pos = Offset(junta.x + (inicio.x - junta.x) * fracao, junta.y + (inicio.y - junta.y) * fracao)
                val cor = corDeHex(t.trainColorHex)
                drawRoundRect(
                    cor, Offset(pos.x - 14.dp.toPx(), pos.y - 7.dp.toPx()), Size(28.dp.toPx(), 14.dp.toPx()),
                    androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
                rotulo(medidor, "${t.prefix} (${restante}h)", Offset(pos.x - 20.dp.toPx(), pos.y + 9.dp.toPx() * if (t.eixoMrs) -3.2f else 1f), corTexto)
            }

            // moegas
            ramais.sortedBy { it.loopAnglePosition }.forEachIndexed { i, r ->
                val pos = pontoNoAnel(cx, cy, rx, ry, r.loopAnglePosition)
                val ang = Math.toRadians(RailRules.anguloDoAnel(r.loopAnglePosition))
                val fora = Offset(pos.x + (cos(ang) * 16.dp.toPx()).toFloat(), pos.y - (sin(ang) * 16.dp.toPx()).toFloat())
                val cor = if (r.switchActive) Paleta.Online else Paleta.Offline
                drawCircle(Color.White, 7.dp.toPx(), pos)
                drawCircle(cor, 5.dp.toPx(), pos)
                rotulo(medidor, (i + 1).toString(), Offset(fora.x - 3.dp.toPx(), fora.y - 7.dp.toPx()), corTexto, negrito = true)
            }

            // centro
            rotulo(medidor, estado.queryHourFormatted, Offset(cx - 22.dp.toPx(), cy - 16.dp.toPx()), corTexto, negrito = true, tamanho = 16)
            rotulo(medidor, "Anel da Pera", Offset(cx - 28.dp.toPx(), cy + 6.dp.toPx()), corSuave)
        }
    }
}

private fun pontoNoAnel(cx: Float, cy: Float, rx: Float, ry: Float, posicao: Double): Offset {
    val a = Math.toRadians(RailRules.anguloDoAnel(posicao))
    return Offset(cx + (rx * cos(a)).toFloat(), cy - (ry * sin(a)).toFloat())
}

private fun DrawScope.seta(cx: Float, cy: Float, rx: Float, ry: Float, posicao: Double, cor: Color) {
    val a = Math.toRadians(RailRules.anguloDoAnel(posicao))
    val p = pontoNoAnel(cx, cy, rx, ry, posicao)
    var tx = (-rx * sin(a)).toFloat()
    var ty = (-ry * cos(a)).toFloat()
    val n = sqrt(tx * tx + ty * ty)
    tx /= n; ty /= n
    val nx = -ty
    val ny = tx
    val c = 8.dp.toPx()
    val l = 5.dp.toPx()
    val caminho = Path().apply {
        moveTo(p.x + tx * c, p.y + ty * c)
        lineTo(p.x - tx * l + nx * l, p.y - ty * l + ny * l)
        lineTo(p.x - tx * l - nx * l, p.y - ty * l - ny * l)
        close()
    }
    drawPath(caminho, cor)
}

private fun DrawScope.rotulo(
    medidor: TextMeasurer,
    texto: String,
    posicao: Offset,
    cor: Color,
    negrito: Boolean = false,
    tamanho: Int = 10
) {
    drawText(
        medidor, texto, posicao,
        style = TextStyle(
            fontSize = tamanho.sp,
            color = cor,
            fontWeight = if (negrito) androidx.compose.ui.text.font.FontWeight.SemiBold else null
        )
    )
}
