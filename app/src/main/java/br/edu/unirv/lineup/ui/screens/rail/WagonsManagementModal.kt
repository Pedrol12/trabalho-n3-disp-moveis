package br.edu.unirv.lineup.ui.screens.rail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.data.model.Locomotive
import br.edu.unirv.lineup.data.model.Wagon
import br.edu.unirv.lineup.data.model.WagonRequest
import br.edu.unirv.lineup.ui.components.CampoSelecao
import br.edu.unirv.lineup.ui.components.ConfirmDeleteDialog
import br.edu.unirv.lineup.ui.components.EstadoVazio

private const val TODAS = "Todas as locomotivas"

/**
 * RF014: modal de gestão de vagões. Dropdown de locomotiva, lista interna rolável, "+ Vagão"
 * e remoção individual. Pode abrir já selecionado em [locomotivaInicial].
 */
@Composable
fun WagonsManagementModal(
    locomotivas: List<Locomotive>,
    vagoes: List<Wagon>,
    locomotivaInicial: String?,
    aoSalvarVagao: (WagonRequest, Boolean) -> Unit,
    aoExcluirVagao: (String) -> Unit,
    aoFechar: () -> Unit
) {
    var selecionada by rememberSaveable { mutableStateOf(locomotivaInicial ?: locomotivas.firstOrNull()?.locomotiveCode) }
    var novoAberto by rememberSaveable { mutableStateOf(false) }
    var editando by rememberSaveable { mutableStateOf<String?>(null) }
    var excluindo by rememberSaveable { mutableStateOf<String?>(null) }

    val opcoes = listOf(TODAS) + locomotivas.map { it.locomotiveCode }
    val lista = if (selecionada == null) vagoes else vagoes.filter { it.locomotiveCode == selecionada }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Gestão de vagões") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoSelecao(
                    rotulo = "Locomotiva",
                    valor = selecionada ?: TODAS,
                    opcoes = opcoes,
                    aoSelecionar = { i -> selecionada = if (i == 0) null else opcoes[i] }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (selecionada == null) "Escolha uma locomotiva para incluir vagões"
                        else "${lista.size} vagão(ões)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { novoAberto = true }, enabled = selecionada != null) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Vagão")
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (lista.isEmpty()) item { EstadoVazio("Nenhum vagão nesta locomotiva.") }
                    items(lista, key = { it.wagonCode }) { v ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text(v.wagonCode, style = MaterialTheme.typography.titleSmall)
                                    Text(v.product, style = MaterialTheme.typography.bodyMedium)
                                    Text(v.capacity, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${v.originTerminal} → ${v.destinationTerminal}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                IconButton(onClick = { editando = v.wagonCode }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar vagão")
                                }
                                IconButton(onClick = { excluindo = v.wagonCode }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remover vagão", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar") } }
    )

    if (novoAberto) WagonDialog(null, selecionada, aoSalvarVagao) { novoAberto = false }
    vagoes.firstOrNull { it.wagonCode == editando }?.let { alvo ->
        WagonDialog(alvo, alvo.locomotiveCode, aoSalvarVagao) { editando = null }
    }
    vagoes.firstOrNull { it.wagonCode == excluindo }?.let { alvo ->
        ConfirmDeleteDialog(
            titulo = "Remover vagão",
            texto = "Remover o vagão ${alvo.wagonCode}?",
            aoConfirmar = {
                aoExcluirVagao(alvo.wagonCode)
                excluindo = null
            },
            aoCancelar = { excluindo = null }
        )
    }
}

@Composable
private fun WagonDialog(
    vagao: Wagon?,
    locomotiva: String?,
    aoSalvar: (WagonRequest, Boolean) -> Unit,
    aoFechar: () -> Unit
) {
    val chave = vagao?.wagonCode ?: "novo"
    var codigo by rememberSaveable(chave) { mutableStateOf(vagao?.wagonCode.orEmpty()) }
    var produto by rememberSaveable(chave) { mutableStateOf(vagao?.product.orEmpty()) }
    var capacidade by rememberSaveable(chave) { mutableStateOf(vagao?.capacity.orEmpty()) }
    var origem by rememberSaveable(chave) { mutableStateOf(vagao?.originTerminal.orEmpty()) }
    var destino by rememberSaveable(chave) { mutableStateOf(vagao?.destinationTerminal.orEmpty()) }
    var erro by rememberSaveable(chave) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (vagao == null) "Novo vagão" else "Editar ${vagao.wagonCode}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Locomotiva: ${locomotiva ?: "nenhuma"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CampoRail(codigo, { codigo = it }, "Código do vagão", habilitado = vagao == null)
                CampoRail(produto, { produto = it }, "Produto")
                CampoRail(capacidade, { capacidade = it }, "Capacidade (ex.: 105,0 ton (Hopper HPT))")
                CampoRail(origem, { origem = it }, "Terminal de origem")
                CampoRail(destino, { destino = it }, "Terminal de destino")
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (listOf(codigo, produto, capacidade, origem, destino).any { it.isBlank() }) {
                    erro = "Preencha todos os campos."
                } else {
                    aoSalvar(
                        WagonRequest(codigo.trim(), produto.trim(), capacidade.trim(), origem.trim(), destino.trim(), locomotiva),
                        vagao == null
                    )
                    aoFechar()
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}

/** Tela "Vagões" (menu e atalho "Modal Vagões" da gaveta): abre o modal automaticamente. */
@Composable
fun RailWagonsScreen(
    locomotivas: List<Locomotive>,
    vagoes: List<Wagon>,
    aoSalvarVagao: (WagonRequest, Boolean) -> Unit,
    aoExcluirVagao: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var modalAberto by rememberSaveable { mutableStateOf(true) }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { modalAberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Abrir gestão de vagões")
        }
        Text(
            "${vagoes.size} vagões em ${locomotivas.size} locomotivas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        locomotivas.forEach { l ->
            Text(
                "${l.locomotiveCode}: ${vagoes.count { it.locomotiveCode == l.locomotiveCode }} vagão(ões)",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    if (modalAberto) {
        WagonsManagementModal(locomotivas, vagoes, null, aoSalvarVagao, aoExcluirVagao) { modalAberto = false }
    }
}
