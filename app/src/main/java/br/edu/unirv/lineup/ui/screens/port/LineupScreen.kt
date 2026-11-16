package br.edu.unirv.lineup.ui.screens.port

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.KpisLineup
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.BerthRequest
import br.edu.unirv.lineup.data.model.CargoCategory
import br.edu.unirv.lineup.data.model.PortCargo
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.ShipScheduleRequest
import br.edu.unirv.lineup.data.model.TideTable
import br.edu.unirv.lineup.ui.components.BerthDialog
import br.edu.unirv.lineup.ui.components.CampoBusca
import br.edu.unirv.lineup.ui.components.CartaoKpi
import br.edu.unirv.lineup.ui.components.EstadoVazio
import br.edu.unirv.lineup.ui.components.Etiqueta
import br.edu.unirv.lineup.ui.components.LinhaDoTempoBerco
import br.edu.unirv.lineup.ui.components.ShipMooringDialog
import br.edu.unirv.lineup.ui.theme.Paleta
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LineupScreen(
    kpis: KpisLineup,
    bercosDaPagina: List<Berth>,
    todosBercos: List<Berth>,
    navios: List<RegisteredVessel>,
    cargas: List<PortCargo>,
    mares: List<TideTable>,
    mareAtiva: TideTable?,
    diasMareBaixa: Set<Int>,
    busca: String,
    categoriasSelecionadas: List<CargoCategory>,
    pagina: Int,
    totalPaginas: Int,
    janelaValida: (ShipSchedule, Double, Double) -> Boolean,
    aoBuscar: (String) -> Unit,
    aoAlternarCategoria: (CargoCategory) -> Unit,
    aoLimparCategorias: () -> Unit,
    aoAtivarMare: (Long) -> Unit,
    aoMudarPagina: (Int) -> Unit,
    aoSalvarAtracacao: (Long?, ShipScheduleRequest) -> Boolean,
    aoExcluirAtracacao: (Long) -> Unit,
    aoMoverAtracacao: (ShipSchedule, Double, Double) -> Unit,
    aoSalvarBerco: (BerthRequest, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var atracarAberto by rememberSaveable { mutableStateOf(false) }
    var atracarBercoId by rememberSaveable { mutableStateOf("") }
    var editandoAtracacaoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var novoBercoAberto by rememberSaveable { mutableStateOf(false) }

    val atracacaoEditada = editandoAtracacaoId?.let { id ->
        todosBercos.flatMap { b -> b.schedules.map { it.copy(berthId = b.id, berthName = b.name) } }.firstOrNull { it.id == id }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CartaoKpi("Berços no filtro", kpis.bercos.toString(), Modifier.weight(1f))
                CartaoKpi("Navios", kpis.navios.toString(), Modifier.weight(1f))
                CartaoKpi("Toneladas", formatarToneladas(kpis.toneladas), Modifier.weight(1.3f))
            }
        }
        item { SeletorDeMare(mares, mareAtiva, aoAtivarMare) }
        item { CampoBusca(busca, aoBuscar, "Buscar berço, navio, produto ou status") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = categoriasSelecionadas.isEmpty(),
                        onClick = aoLimparCategorias,
                        label = { Text("Todas") }
                    )
                }
                items(CargoCategory.entries.toList()) { cat ->
                    FilterChip(
                        selected = cat in categoriasSelecionadas,
                        onClick = { aoAlternarCategoria(cat) },
                        label = { Text(cat.rotulo) },
                        leadingIcon = {
                            Box(Modifier.size(10.dp).background(cat.cor, RoundedCornerShape(50)))
                        }
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        atracarBercoId = bercosDaPagina.firstOrNull()?.id ?: todosBercos.firstOrNull()?.id.orEmpty()
                        atracarAberto = true
                    },
                    enabled = todosBercos.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Atracar Navio")
                }
                OutlinedButton(onClick = { novoBercoAberto = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Novo Berço")
                }
            }
        }
        item { LegendaStatus() }

        if (bercosDaPagina.isEmpty()) {
            item { EstadoVazio("Nenhum berço encontrado para o filtro atual.") }
        }
        items(bercosDaPagina, key = { it.id }) { berco ->
            CartaoBerco(
                berco = berco,
                mare = mareAtiva,
                diasMareBaixa = diasMareBaixa,
                janelaValida = janelaValida,
                aoTocarNavio = { editandoAtracacaoId = it.id },
                aoMover = aoMoverAtracacao,
                aoAtracar = {
                    atracarBercoId = berco.id
                    atracarAberto = true
                }
            )
        }
        item { Paginacao(pagina, totalPaginas, aoMudarPagina) }
    }

    if (atracarAberto) {
        ShipMooringDialog(
            atracacao = null,
            bercoInicialId = atracarBercoId,
            bercos = todosBercos,
            navios = navios,
            cargas = cargas,
            diasMareBaixa = diasMareBaixa,
            aoSalvar = aoSalvarAtracacao,
            aoExcluir = null,
            aoFechar = { atracarAberto = false }
        )
    }
    if (atracacaoEditada != null) {
        ShipMooringDialog(
            atracacao = atracacaoEditada,
            bercoInicialId = atracacaoEditada.berthId,
            bercos = todosBercos,
            navios = navios,
            cargas = cargas,
            diasMareBaixa = diasMareBaixa,
            aoSalvar = aoSalvarAtracacao,
            aoExcluir = aoExcluirAtracacao,
            aoFechar = { editandoAtracacaoId = null }
        )
    }
    if (novoBercoAberto) {
        BerthDialog(berco = null, aoSalvar = aoSalvarBerco, aoFechar = { novoBercoAberto = false })
    }
}

private fun formatarToneladas(valor: Long): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(valor)

@Composable
private fun SeletorDeMare(mares: List<TideTable>, ativa: TideTable?, aoAtivar: (Long) -> Unit) {
    var aberto by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { aberto = true }, enabled = mares.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(
                "Tábua de marés ativa: ${ativa?.monthLabel ?: "nenhuma"}",
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            mares.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.monthLabel) },
                    trailingIcon = { if (t.activeMonth) Icon(Icons.Default.Check, contentDescription = "Ativa") },
                    onClick = {
                        aberto = false
                        t.id?.let(aoAtivar)
                    }
                )
            }
        }
    }
}

@Composable
private fun LegendaStatus() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        ItemLegenda("Operando / Prioridade", androidx.compose.ui.graphics.Color(0xFFE74C3C))
        ItemLegenda("Programado", androidx.compose.ui.graphics.Color(0xFFE67E22))
        ItemLegenda("Confirmado", androidx.compose.ui.graphics.Color(0xFF2ECC71))
        ItemLegenda("Maré baixa", Paleta.MareBaixa)
    }
}

@Composable
private fun ItemLegenda(texto: String, cor: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(cor, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(5.dp))
        Text(texto, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CartaoBerco(
    berco: Berth,
    mare: TideTable?,
    diasMareBaixa: Set<Int>,
    janelaValida: (ShipSchedule, Double, Double) -> Boolean,
    aoTocarNavio: (ShipSchedule) -> Unit,
    aoMover: (ShipSchedule, Double, Double) -> Unit,
    aoAtracar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${berco.id}  ${berco.name}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${berco.sectorName}  |  Calado ${berco.maxDraft}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Etiqueta(berco.category.rotulo, berco.category.cor)
            }
            LinhaDoTempoBerco(
                atracacoes = berco.schedules.map { it.copy(berthId = berco.id, berthName = berco.name) },
                mare = mare,
                diasMareBaixa = diasMareBaixa,
                janelaValida = janelaValida,
                aoTocar = aoTocarNavio,
                aoMover = aoMover
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${berco.schedules.size} navio(s) programado(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.TextButton(onClick = aoAtracar) { Text("Atracar neste berço") }
            }
        }
    }
}

@Composable
private fun Paginacao(pagina: Int, total: Int, aoMudar: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = { aoMudar(pagina - 1) }, enabled = pagina > 1) { Text("Anterior") }
        Text("Página $pagina de $total", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { aoMudar(pagina + 1) }, enabled = pagina < total) { Text("Próxima") }
    }
    Spacer(Modifier.height(16.dp))
}
