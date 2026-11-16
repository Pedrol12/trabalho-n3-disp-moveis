package br.edu.unirv.lineup.ui.screens.highway

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import br.edu.unirv.lineup.data.model.HighwayLine
import br.edu.unirv.lineup.data.model.HighwayRules
import br.edu.unirv.lineup.data.model.RegisteredTruck
import br.edu.unirv.lineup.data.model.TruckSchedule
import br.edu.unirv.lineup.data.model.corDeHex
import br.edu.unirv.lineup.ui.components.CartaoKpi
import br.edu.unirv.lineup.ui.components.EstadoVazio
import br.edu.unirv.lineup.ui.theme.Paleta
import kotlin.math.max
import kotlin.math.min

/** Docas por página do Gantt (RNF001: 180 docas / 5 = 36 páginas). */
const val DOCAS_POR_PAGINA = 5
private val LARGURA_SLOT = 12.dp
private val ALTURA_LINHA = 44.dp
private val ALTURA_REGUA = 22.dp

/** Índice (0-based) do slot tocado a partir da coordenada x do toque, sem depender de Compose (testável). */
fun slotDoToque(xPx: Float, larguraSlotPx: Float, dia: Int): Int {
    val noDia = (xPx / larguraSlotPx).toInt().coerceIn(0, HighwayRules.SLOTS_POR_DIA - 1)
    return (dia - 1) * HighwayRules.SLOTS_POR_DIA + noDia
}

/** RF017: Gantt de 180 docas, 96 slots de 15 min por dia, 5 docas por página. */
@Composable
fun HighwayGanttScreen(
    docas: List<HighwayLine>,
    cadastrados: List<RegisteredTruck>,
    aoSalvar: (Int, TruckSchedule) -> Boolean,
    aoExcluir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var dia by rememberSaveable { mutableIntStateOf(1) }
    var pagina by rememberSaveable { mutableIntStateOf(1) }
    var editandoId by rememberSaveable { mutableStateOf<String?>(null) }
    var novaNaDoca by rememberSaveable { mutableStateOf<Int?>(null) }
    val rolagem = rememberScrollState()

    val totalPaginas = max(1, (docas.size + DOCAS_POR_PAGINA - 1) / DOCAS_POR_PAGINA)
    val paginaAtual = pagina.coerceIn(1, totalPaginas)
    val daPagina = docas.drop((paginaAtual - 1) * DOCAS_POR_PAGINA).take(DOCAS_POR_PAGINA)
    val totalAgendamentos = docas.sumOf { it.trucks.size }
    val inicioDia = (dia - 1) * HighwayRules.SLOTS_POR_DIA
    val fimDia = inicioDia + HighwayRules.SLOTS_POR_DIA
    val noDia = docas.sumOf { d -> d.trucks.count { it.startSlot < fimDia && it.endSlot > inicioDia } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CartaoKpi("Docas", docas.size.toString(), Modifier.weight(1f))
                CartaoKpi("Agendamentos", totalAgendamentos.toString(), Modifier.weight(1f))
                CartaoKpi("No dia D$dia", noDia.toString(), Modifier.weight(1f))
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((1..HighwayRules.DIAS).toList()) { d ->
                    FilterChip(selected = d == dia, onClick = { dia = d }, label = { Text("D$d") })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { pagina = paginaAtual - 1 }, enabled = paginaAtual > 1) { Text("Anterior") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Página $paginaAtual de $totalPaginas", style = MaterialTheme.typography.labelLarge)
                    if (daPagina.isNotEmpty()) {
                        Text(
                            "Docas ${daPagina.first().lineNumber} a ${daPagina.last().lineNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(onClick = { pagina = paginaAtual + 1 }, enabled = paginaAtual < totalPaginas) { Text("Próxima") }
            }
        }
        item { ReguaDeHoras(rolagem) }
        if (daPagina.isEmpty()) item { EstadoVazio("Nenhuma doca carregada. Conecte ao backend e atualize.") }
        items(daPagina, key = { it.lineNumber }) { doca ->
            LinhaDaDoca(
                doca = doca,
                dia = dia,
                rolagem = rolagem,
                aoTocarBarra = { editandoId = it.truckId },
                aoAdicionar = { novaNaDoca = doca.lineNumber }
            )
        }
    }

    novaNaDoca?.let { numero ->
        TruckAppointmentDialog(
            agendamento = null, docaInicial = numero, diaInicial = dia, docas = docas, cadastrados = cadastrados,
            aoSalvar = aoSalvar, aoExcluir = null, aoFechar = { novaNaDoca = null }
        )
    }
    val alvo = docas.firstNotNullOfOrNull { d -> d.trucks.firstOrNull { it.truckId == editandoId } }
    if (alvo != null) {
        TruckAppointmentDialog(
            agendamento = alvo, docaInicial = alvo.lineNumber, diaInicial = dia, docas = docas, cadastrados = cadastrados,
            aoSalvar = aoSalvar, aoExcluir = aoExcluir, aoFechar = { editandoId = null }
        )
    }
}

@Composable
private fun ReguaDeHoras(rolagem: ScrollState) {
    val medidor = rememberTextMeasurer()
    val corTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val corGrade = MaterialTheme.colorScheme.outline
    val estilo = remember(corTexto) { TextStyle(fontSize = 10.sp, color = corTexto, fontWeight = FontWeight.Medium) }
    val rotulos = remember(estilo) { (0..23).map { medidor.measure("%02d".format(it) + "h", estilo) } }
    Box(Modifier.horizontalScroll(rolagem)) {
        Canvas(Modifier.width(LARGURA_SLOT * HighwayRules.SLOTS_POR_DIA).height(ALTURA_REGUA)) {
            val w = LARGURA_SLOT.toPx()
            for (h in 0..23) {
                val x = h * HighwayRules.SLOTS_POR_HORA * w
                drawLine(corGrade, Offset(x, size.height * 0.55f), Offset(x, size.height), 1f)
                drawText(rotulos[h], topLeft = Offset(x + 3.dp.toPx(), 1.dp.toPx()))
            }
        }
    }
}

@Composable
private fun LinhaDaDoca(
    doca: HighwayLine,
    dia: Int,
    rolagem: ScrollState,
    aoTocarBarra: (TruckSchedule) -> Unit,
    aoAdicionar: () -> Unit
) {
    val corSetor = corDeHex(doca.sectorColorHex)
    val inicioDia = (dia - 1) * HighwayRules.SLOTS_POR_DIA
    val fimDia = inicioDia + HighwayRules.SLOTS_POR_DIA
    val doDia = remember(doca.trucks, dia) { doca.trucks.filter { it.startSlot < fimDia && it.endSlot > inicioDia } }
    val doDiaAtual by rememberUpdatedState(doDia)
    val aoTocarAtual by rememberUpdatedState(aoTocarBarra)
    val medidor = rememberTextMeasurer()
    val corGrade = MaterialTheme.colorScheme.outline
    val corFundo = MaterialTheme.colorScheme.surface
    val estilo = remember { TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(start = 12.dp, top = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(doca.lineCode, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${doca.sectorName.substringBefore(" • Gates").ifBlank { doca.sectorName }} · ${doDia.size} no dia · ${doca.truckCount} no mês",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = aoAdicionar) {
                Icon(Icons.Default.Add, contentDescription = "Novo agendamento na ${doca.lineCode}", tint = corSetor)
            }
        }
        Box(Modifier.padding(bottom = 8.dp).horizontalScroll(rolagem)) {
            Canvas(
                Modifier
                    .width(LARGURA_SLOT * HighwayRules.SLOTS_POR_DIA)
                    .height(ALTURA_LINHA)
                    .pointerInput(dia) {
                        detectTapGestures { toque ->
                            val slot = slotDoToque(toque.x, LARGURA_SLOT.toPx(), dia)
                            doDiaAtual.firstOrNull { slot >= it.startSlot && slot < it.endSlot }?.let(aoTocarAtual)
                        }
                    }
            ) {
                val w = LARGURA_SLOT.toPx()
                drawRect(corFundo, size = size)
                for (h in 0..24) {
                    val x = h * HighwayRules.SLOTS_POR_HORA * w
                    drawLine(corGrade, Offset(x, 0f), Offset(x, size.height), if (h % 6 == 0) 1.5f else 0.7f)
                }
                val margem = 3.dp.toPx()
                doDia.forEach { t ->
                    val ini = max(t.startSlot, inicioDia) - inicioDia
                    val fim = min(t.endSlot, fimDia) - inicioDia
                    val x = ini * w
                    val largura = (fim - ini) * w
                    val cor = corDeHex(t.colorHex, corSetor)
                    drawRoundRect(
                        cor, Offset(x + 1f, margem), Size(largura - 2f, size.height - 2 * margem),
                        CornerRadius(6.dp.toPx())
                    )
                    val textoCor = if (cor.luminance() > 0.55f) Paleta.Texto else Color.White
                    val maxLargura = (largura - 8.dp.toPx()).toInt()
                    if (maxLargura > 24.dp.toPx()) {
                        val placa = t.plateAndFleet.substringBefore("•").trim()
                        val frota = t.plateAndFleet.substringAfter("#", "").trim()
                        val rotulo = if (frota.isNotEmpty() && maxLargura > 110.dp.toPx()) "$placa  F$frota" else placa
                        val texto = medidor.measure(
                            rotulo, estilo.copy(color = textoCor), overflow = TextOverflow.Ellipsis, softWrap = false,
                            constraints = Constraints(maxWidth = maxLargura)
                        )
                        drawText(texto, topLeft = Offset(x + 6.dp.toPx(), (size.height - texto.size.height) / 2f))
                    }
                }
            }
        }
    }
}
