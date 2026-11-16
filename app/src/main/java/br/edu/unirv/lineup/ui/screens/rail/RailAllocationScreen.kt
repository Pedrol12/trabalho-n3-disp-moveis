package br.edu.unirv.lineup.ui.screens.rail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.data.model.Locomotive
import br.edu.unirv.lineup.data.model.RailRules
import br.edu.unirv.lineup.data.model.Wagon
import br.edu.unirv.lineup.data.model.WagonAllocation
import br.edu.unirv.lineup.ui.components.CampoBusca
import br.edu.unirv.lineup.ui.components.CampoSelecao
import br.edu.unirv.lineup.ui.components.CartaoItem
import br.edu.unirv.lineup.ui.components.ConfirmDeleteDialog
import br.edu.unirv.lineup.ui.components.EstadoVazio

/** RF015: ordem sequencial de vagões por locomotiva. */
@Composable
fun RailAllocationScreen(
    alocacoes: List<WagonAllocation>,
    locomotivas: List<Locomotive>,
    vagoes: List<Wagon>,
    aoSalvar: (WagonAllocation, Boolean) -> Unit,
    aoExcluir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var dialogoNovo by rememberSaveable { mutableStateOf(false) }
    var editando by rememberSaveable { mutableStateOf<String?>(null) }
    var excluindo by rememberSaveable { mutableStateOf<String?>(null) }

    val q = busca.trim().lowercase()
    val filtradas = RailRules.ordenarAlocacoes(alocacoes).filter {
        q.isEmpty() || it.locomotiveCode.lowercase().contains(q) || it.wagonCode.lowercase().contains(q) ||
            it.product.lowercase().contains(q) || it.destinationTerminal.lowercase().contains(q) ||
            it.positionOrder.lowercase().contains(q)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { CampoBusca(busca, { busca = it }, "Buscar por vagão, locomotiva ou mercadoria") }
        item {
            Button(onClick = { dialogoNovo = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nova alocação")
            }
        }
        if (filtradas.isEmpty()) item { EstadoVazio("Nenhuma alocação encontrada.") }
        items(filtradas, key = { it.allocationId }) { a ->
            CartaoItem(
                titulo = "${a.positionOrder}  |  ${a.wagonCode}",
                subtitulo = a.locomotiveCode,
                detalhes = listOf(
                    "Mercadoria: ${a.product}",
                    "Peso: ${a.capacity}",
                    "Moega destino: ${a.destinationTerminal}"
                ),
                aoEditar = { editando = a.allocationId },
                aoExcluir = { excluindo = a.allocationId }
            )
        }
    }

    if (dialogoNovo) AllocationDialog(null, alocacoes, locomotivas, vagoes, aoSalvar) { dialogoNovo = false }
    alocacoes.firstOrNull { it.allocationId == editando }?.let { alvo ->
        AllocationDialog(alvo, alocacoes, locomotivas, vagoes, aoSalvar) { editando = null }
    }
    alocacoes.firstOrNull { it.allocationId == excluindo }?.let { alvo ->
        ConfirmDeleteDialog(
            titulo = "Excluir alocação",
            texto = "Excluir ${alvo.positionOrder} (${alvo.wagonCode}) de ${alvo.locomotiveCode}?",
            aoConfirmar = {
                aoExcluir(alvo.allocationId)
                excluindo = null
            },
            aoCancelar = { excluindo = null }
        )
    }
}

@Composable
private fun AllocationDialog(
    alocacao: WagonAllocation?,
    todas: List<WagonAllocation>,
    locomotivas: List<Locomotive>,
    vagoes: List<Wagon>,
    aoSalvar: (WagonAllocation, Boolean) -> Unit,
    aoFechar: () -> Unit
) {
    val chave = alocacao?.allocationId ?: "nova"
    var locomotiva by rememberSaveable(chave) {
        mutableStateOf(alocacao?.locomotiveCode ?: locomotivas.firstOrNull()?.locomotiveCode.orEmpty())
    }
    var codigoVagao by rememberSaveable(chave) { mutableStateOf(alocacao?.wagonCode.orEmpty()) }
    var posicao by rememberSaveable(chave) {
        mutableStateOf(alocacao?.positionOrder ?: RailRules.proximaPosicao(todas, locomotiva))
    }
    var erro by rememberSaveable(chave) { mutableStateOf<String?>(null) }

    val vagoesDaLocomotiva = vagoes.filter { it.locomotiveCode == locomotiva }
    val vagaoEscolhido = vagoes.firstOrNull { it.wagonCode == codigoVagao }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (alocacao == null) "Nova alocação" else "Editar ${alocacao.allocationId}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoSelecao(
                    rotulo = "Locomotiva",
                    valor = locomotiva,
                    opcoes = locomotivas.map { it.locomotiveCode },
                    aoSelecionar = { i ->
                        locomotiva = locomotivas[i].locomotiveCode
                        codigoVagao = ""
                        if (alocacao == null) posicao = RailRules.proximaPosicao(todas, locomotiva)
                    }
                )
                CampoSelecao(
                    rotulo = "Vagão",
                    valor = codigoVagao,
                    opcoes = vagoesDaLocomotiva.map { it.wagonCode },
                    aoSelecionar = { codigoVagao = vagoesDaLocomotiva[it].wagonCode }
                )
                CampoRail(posicao, { posicao = it }, "Posição (ex.: Posição #001)")
                vagaoEscolhido?.let {
                    Text("Mercadoria: ${it.product}", style = MaterialTheme.typography.bodySmall)
                    Text("Peso: ${it.capacity}", style = MaterialTheme.typography.bodySmall)
                    Text("Moega destino: ${it.destinationTerminal}", style = MaterialTheme.typography.bodySmall)
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val vagao = vagaoEscolhido ?: alocacao?.takeIf { it.wagonCode == codigoVagao }?.let {
                    Wagon(it.wagonCode, it.product, it.capacity, it.originTerminal, it.destinationTerminal, it.locomotiveCode)
                }
                when {
                    locomotiva.isBlank() -> erro = "Selecione a locomotiva."
                    vagao == null -> erro = "Selecione um vagão da locomotiva."
                    posicao.isBlank() -> erro = "Informe a posição."
                    else -> {
                        aoSalvar(
                            WagonAllocation(
                                allocationId = alocacao?.allocationId.orEmpty(),
                                locomotiveCode = locomotiva,
                                positionOrder = posicao.trim(),
                                wagonCode = vagao.wagonCode,
                                product = vagao.product,
                                capacity = vagao.capacity,
                                originTerminal = vagao.originTerminal,
                                destinationTerminal = vagao.destinationTerminal
                            ),
                            alocacao == null
                        )
                        aoFechar()
                    }
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
