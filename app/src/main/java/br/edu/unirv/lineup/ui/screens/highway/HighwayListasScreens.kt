package br.edu.unirv.lineup.ui.screens.highway

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.data.model.HighwayCargo
import br.edu.unirv.lineup.data.model.HighwayLine
import br.edu.unirv.lineup.data.model.HighwayRules
import br.edu.unirv.lineup.data.model.HighwaySector
import br.edu.unirv.lineup.data.model.RegisteredTruck
import br.edu.unirv.lineup.data.model.TruckSchedule
import br.edu.unirv.lineup.data.model.corDeHex
import br.edu.unirv.lineup.ui.components.CampoBusca
import br.edu.unirv.lineup.ui.components.CartaoItem
import br.edu.unirv.lineup.ui.components.CartaoKpi
import br.edu.unirv.lineup.ui.components.ConfirmDeleteDialog
import br.edu.unirv.lineup.ui.components.EstadoVazio
import br.edu.unirv.lineup.ui.components.Etiqueta
import br.edu.unirv.lineup.ui.theme.Paleta

private val CorDescarga = Color(0xFF1D5FA8)
private val CorRetorno = Color(0xFFD9822B)

@Composable
private fun BotaoNovo(texto: String, aoClicar: () -> Unit) {
    Button(onClick = aoClicar, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto)
    }
}

/** RF018: as 180 docas com setor, perfil de carga e total de caminhões no mês. */
@Composable
fun HighwayDocksScreen(docas: List<HighwayLine>, modifier: Modifier = Modifier) {
    var busca by rememberSaveable { mutableStateOf("") }
    val q = busca.trim().lowercase()
    val filtradas = remember(docas, q) {
        docas.filter {
            q.isEmpty() || it.lineCode.lowercase().contains(q) || it.sectorName.lowercase().contains(q) ||
                it.cargoProfile.lowercase().contains(q)
        }
    }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { CampoBusca(busca, { busca = it }, "Buscar por doca, setor ou perfil de carga") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CartaoKpi("Docas", filtradas.size.toString(), Modifier.weight(1f))
                CartaoKpi("Caminhões no mês", filtradas.sumOf { it.truckCount }.toString(), Modifier.weight(1f))
            }
        }
        if (filtradas.isEmpty()) item { EstadoVazio("Nenhuma doca encontrada.") }
        items(filtradas, key = { it.lineNumber }) { d ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(d.lineCode, style = MaterialTheme.typography.titleMedium)
                    Text(d.cargoProfile, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                    Etiqueta(d.sectorName, corDeHex(d.sectorColorHex), Modifier.padding(top = 6.dp))
                    Text(
                        "Caminhões no mês: ${d.truckCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

/** RF019: setores e Ecopátio regulador, com a faixa de docas calculada a partir das linhas do Gantt. */
@Composable
fun HighwaySectorsScreen(setores: List<HighwaySector>, docas: List<HighwayLine>, modifier: Modifier = Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (setores.isEmpty()) item { EstadoVazio("Nenhum setor carregado.") }
        items(setores, key = { it.sectorCode }) { s ->
            val linhas = docas.filter { it.sectorName == s.sectorName }.map { it.lineNumber }
            val faixa = if (linhas.isEmpty()) "Pátio regulador (${s.gateCount} docas controladas)"
            else "Docas ${linhas.min()} a ${linhas.max()} (${s.gateCount} gates)"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(s.sectorName, style = MaterialTheme.typography.titleMedium)
                    Text(s.sectorCode, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Via de acesso: ${s.accessHighway}", style = MaterialTheme.typography.bodyMedium)
                    Text("Perfil: ${s.vehicleProfile}", style = MaterialTheme.typography.bodyMedium)
                    Text(faixa, style = MaterialTheme.typography.bodyMedium)
                    Etiqueta(s.operationalStatus, corDeHex(s.colorHex), Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** RF020: CRUD de caminhões cadastrados. */
@Composable
fun HighwayTrucksScreen(
    caminhoes: List<RegisteredTruck>,
    aoSalvar: (RegisteredTruck, Boolean) -> Unit,
    aoExcluir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var dialogoNovo by rememberSaveable { mutableStateOf(false) }
    var editando by rememberSaveable { mutableStateOf<String?>(null) }
    var excluindo by rememberSaveable { mutableStateOf<String?>(null) }
    val q = busca.trim().lowercase()
    val filtrados = caminhoes.filter {
        q.isEmpty() || it.truckCode.lowercase().contains(q) || it.plateAndFleet.lowercase().contains(q) ||
            it.carrierName.lowercase().contains(q) || it.truckConfigType.lowercase().contains(q)
    }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { CampoBusca(busca, { busca = it }, "Buscar por placa, frota, transportadora ou código") }
        item { BotaoNovo("Novo caminhão") { dialogoNovo = true } }
        if (filtrados.isEmpty()) item { EstadoVazio("Nenhum caminhão encontrado.") }
        items(filtrados, key = { it.truckCode }) { c ->
            CartaoItem(
                titulo = c.plateAndFleet,
                subtitulo = "${c.truckCode} · ${c.carrierName}",
                detalhes = listOf(
                    "Configuração: ${c.truckConfigType} (${c.maxCapacityTons} t)",
                    "↓ ${c.defaultUnloadProduct}",
                    "↑ ${c.defaultLoadProduct}"
                ),
                aoEditar = { editando = c.truckCode },
                aoExcluir = { excluindo = c.truckCode }
            )
        }
    }
    if (dialogoNovo) RegisteredTruckDialog(null, aoSalvar) { dialogoNovo = false }
    caminhoes.firstOrNull { it.truckCode == editando }?.let { RegisteredTruckDialog(it, aoSalvar) { editando = null } }
    caminhoes.firstOrNull { it.truckCode == excluindo }?.let { alvo ->
        ConfirmDeleteDialog(
            "Excluir caminhão", "Excluir ${alvo.plateAndFleet} (${alvo.truckCode})?",
            { aoExcluir(alvo.truckCode); excluindo = null }, { excluindo = null }
        )
    }
}

/** RF021: cargas rodoviárias, Descarga (azul, ↓) versus Retorno (laranja, ↑). */
@Composable
fun HighwayCargosScreen(
    cargas: List<HighwayCargo>,
    aoSalvar: (HighwayCargo, Boolean) -> Unit,
    aoExcluir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var dialogoNovo by rememberSaveable { mutableStateOf(false) }
    var editando by rememberSaveable { mutableStateOf<String?>(null) }
    var excluindo by rememberSaveable { mutableStateOf<String?>(null) }
    val q = busca.trim().lowercase()
    val filtradas = cargas.filter {
        q.isEmpty() || it.cargoCode.lowercase().contains(q) || it.productName.lowercase().contains(q) ||
            it.equipmentType.lowercase().contains(q)
    }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { CampoBusca(busca, { busca = it }, "Buscar por código, produto ou equipamento") }
        item { BotaoNovo("Nova carga rodoviária") { dialogoNovo = true } }
        if (filtradas.isEmpty()) item { EstadoVazio("Nenhuma carga encontrada.") }
        items(filtradas, key = { it.cargoCode }) { c ->
            val cor = if (c.ehRetorno) CorRetorno else CorDescarga
            CartaoItem(
                titulo = c.productName,
                subtitulo = c.cargoCode,
                detalhes = listOf(
                    "Equipamento: ${c.equipmentType}",
                    "Permanência: ${c.defaultStayDuration}"
                ),
                aoEditar = { editando = c.cargoCode },
                aoExcluir = { excluindo = c.cargoCode },
                etiqueta = { Etiqueta(if (c.ehRetorno) "↑ Retorno" else "↓ Descarga", cor) }
            )
        }
    }
    if (dialogoNovo) HighwayCargoDialog(null, aoSalvar) { dialogoNovo = false }
    cargas.firstOrNull { it.cargoCode == editando }?.let { HighwayCargoDialog(it, aoSalvar) { editando = null } }
    cargas.firstOrNull { it.cargoCode == excluindo }?.let { alvo ->
        ConfirmDeleteDialog(
            "Excluir carga", "Excluir ${alvo.productName} (${alvo.cargoCode})?",
            { aoExcluir(alvo.cargoCode); excluindo = null }, { excluindo = null }
        )
    }
}

/** RF022: lista plana pesquisável de todos os agendamentos das 180 docas. */
@Composable
fun HighwayAppointmentsScreen(
    docas: List<HighwayLine>,
    cadastrados: List<RegisteredTruck>,
    aoSalvar: (Int, TruckSchedule) -> Boolean,
    aoExcluir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var dialogoNovo by rememberSaveable { mutableStateOf(false) }
    var editando by rememberSaveable { mutableStateOf<String?>(null) }
    var excluindo by rememberSaveable { mutableStateOf<String?>(null) }
    val todos = remember(docas) { docas.flatMap { it.trucks } }
    val q = busca.trim().lowercase()
    val filtrados = remember(todos, q) {
        todos.filter {
            q.isEmpty() || it.plateAndFleet.lowercase().contains(q) || it.lineCode.lowercase().contains(q) ||
                it.unloadingProduct.lowercase().contains(q) || it.loadingProduct.lowercase().contains(q) ||
                it.carrierName.lowercase().contains(q)
        }
    }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { CampoBusca(busca, { busca = it }, "Buscar por placa, frota, doca ou produto") }
        item { CartaoKpi("Agendamentos listados", "${filtrados.size} de ${todos.size}") }
        item { BotaoNovo("Novo agendamento") { dialogoNovo = true } }
        if (filtrados.isEmpty()) item { EstadoVazio("Nenhum agendamento encontrado.") }
        items(filtrados, key = { it.truckId }) { t ->
            val slots = t.endSlot - t.startSlot
            CartaoItem(
                titulo = t.plateAndFleet,
                subtitulo = "${t.lineCode} · ${t.carrierName}",
                detalhes = listOf(
                    "Dia %02d · %s às %s · %d slots".format(
                        HighwayRules.diaDoSlot(t.startSlot), HighwayRules.horaFormatada(t.startSlot),
                        HighwayRules.formatarSlot(t.endSlot), slots
                    ),
                    "↓ ${t.unloadingProduct}",
                    "↑ ${t.loadingProduct}",
                    "Carga: ${t.tonnage} t"
                ),
                aoEditar = { editando = t.truckId },
                aoExcluir = { excluindo = t.truckId },
                etiqueta = { Etiqueta(t.lineCode.substringBefore(" ("), corDeHex(t.colorHex, Paleta.Aco)) }
            )
        }
    }
    if (dialogoNovo) {
        TruckAppointmentDialog(null, docas.firstOrNull()?.lineNumber ?: 1, 1, docas, cadastrados, aoSalvar, null) { dialogoNovo = false }
    }
    todos.firstOrNull { it.truckId == editando }?.let { alvo ->
        TruckAppointmentDialog(alvo, alvo.lineNumber, 1, docas, cadastrados, aoSalvar, aoExcluir) { editando = null }
    }
    todos.firstOrNull { it.truckId == excluindo }?.let { alvo ->
        ConfirmDeleteDialog(
            "Excluir agendamento", "Excluir o agendamento de ${alvo.plateAndFleet} na ${alvo.lineCode}?",
            { aoExcluir(alvo.truckId); excluindo = null }, { excluindo = null }
        )
    }
}
