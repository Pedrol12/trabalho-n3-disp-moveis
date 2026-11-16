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
import br.edu.unirv.lineup.data.model.LocomotiveArrival
import br.edu.unirv.lineup.data.model.RailRules
import br.edu.unirv.lineup.ui.components.CampoBusca
import br.edu.unirv.lineup.ui.components.CampoSelecao
import br.edu.unirv.lineup.ui.components.CartaoItem
import br.edu.unirv.lineup.ui.components.ConfirmDeleteDialog
import br.edu.unirv.lineup.ui.components.EstadoVazio
import br.edu.unirv.lineup.ui.components.Etiqueta

private val OPCOES_STATUS = listOf(
    "Concluída / Escoada",
    "Ocupando a Pera (Em Operação)",
    "Em Aproximação (Descendo a Serra)",
    "Programada"
)

/** RF016: janelas de chegada de composições. */
@Composable
fun RailArrivalsScreen(
    chegadas: List<LocomotiveArrival>,
    locomotivas: List<Locomotive>,
    aoSalvar: (LocomotiveArrival, Boolean) -> Unit,
    aoExcluir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var dialogoNovo by rememberSaveable { mutableStateOf(false) }
    var editando by rememberSaveable { mutableStateOf<String?>(null) }
    var excluindo by rememberSaveable { mutableStateOf<String?>(null) }

    val q = busca.trim().lowercase()
    val filtradas = chegadas.filter {
        q.isEmpty() || it.locomotiveCode.lowercase().contains(q) || it.product.lowercase().contains(q) ||
            it.destinationTerminal.lowercase().contains(q) || it.status.lowercase().contains(q) ||
            it.arrivalId.lowercase().contains(q)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { CampoBusca(busca, { busca = it }, "Buscar por locomotiva, produto, terminal ou status") }
        item {
            Button(onClick = { dialogoNovo = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nova chegada")
            }
        }
        if (filtradas.isEmpty()) item { EstadoVazio("Nenhuma chegada encontrada.") }
        items(filtradas, key = { it.arrivalId }) { c ->
            CartaoItem(
                titulo = c.locomotiveCode,
                subtitulo = "${c.arrivalId}  |  ${c.operatorAxis}",
                detalhes = listOf(
                    "${c.dateFormatted}  |  ${c.startTime} às ${c.endTime}",
                    "${c.wagonSummary}  |  ${c.product}",
                    "${c.originTerminal} → ${c.destinationTerminal}"
                ),
                aoEditar = { editando = c.arrivalId },
                aoExcluir = { excluindo = c.arrivalId },
                etiqueta = { Etiqueta(RailRules.categoriaDeStatus(c.status), corDoStatus(c.status)) }
            )
        }
    }

    if (dialogoNovo) ArrivalDialog(null, locomotivas, aoSalvar) { dialogoNovo = false }
    chegadas.firstOrNull { it.arrivalId == editando }?.let { alvo ->
        ArrivalDialog(alvo, locomotivas, aoSalvar) { editando = null }
    }
    chegadas.firstOrNull { it.arrivalId == excluindo }?.let { alvo ->
        ConfirmDeleteDialog(
            titulo = "Excluir chegada",
            texto = "Excluir a chegada ${alvo.arrivalId} (${alvo.locomotiveCode})?",
            aoConfirmar = {
                aoExcluir(alvo.arrivalId)
                excluindo = null
            },
            aoCancelar = { excluindo = null }
        )
    }
}

@Composable
private fun ArrivalDialog(
    chegada: LocomotiveArrival?,
    locomotivas: List<Locomotive>,
    aoSalvar: (LocomotiveArrival, Boolean) -> Unit,
    aoFechar: () -> Unit
) {
    val chave = chegada?.arrivalId ?: "nova"
    var locomotiva by rememberSaveable(chave) {
        mutableStateOf(chegada?.locomotiveCode ?: locomotivas.firstOrNull()?.locomotiveCode.orEmpty())
    }
    var operador by rememberSaveable(chave) {
        mutableStateOf(chegada?.operatorAxis ?: locomotivas.firstOrNull()?.operatorAxis.orEmpty())
    }
    var data by rememberSaveable(chave) { mutableStateOf(chegada?.dateFormatted.orEmpty()) }
    var inicio by rememberSaveable(chave) { mutableStateOf(chegada?.startTime.orEmpty()) }
    var fim by rememberSaveable(chave) { mutableStateOf(chegada?.endTime.orEmpty()) }
    var produto by rememberSaveable(chave) { mutableStateOf(chegada?.product.orEmpty()) }
    var resumo by rememberSaveable(chave) { mutableStateOf(chegada?.wagonSummary.orEmpty()) }
    var origem by rememberSaveable(chave) { mutableStateOf(chegada?.originTerminal.orEmpty()) }
    var destino by rememberSaveable(chave) { mutableStateOf(chegada?.destinationTerminal.orEmpty()) }
    var status by rememberSaveable(chave) { mutableStateOf(chegada?.status ?: OPCOES_STATUS.last()) }
    var erro by rememberSaveable(chave) { mutableStateOf<String?>(null) }

    val opcoesLocomotiva = (locomotivas.map { it.locomotiveCode } + listOfNotNull(chegada?.locomotiveCode)).distinct()

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (chegada == null) "Nova chegada" else "Editar ${chegada.arrivalId}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoSelecao(
                    rotulo = "Locomotiva",
                    valor = locomotiva,
                    opcoes = opcoesLocomotiva,
                    aoSelecionar = { i ->
                        locomotiva = opcoesLocomotiva[i]
                        locomotivas.firstOrNull { it.locomotiveCode == locomotiva }?.let { operador = it.operatorAxis }
                    }
                )
                CampoRail(operador, { operador = it }, "Operador / eixo")
                CampoRail(data, { data = it }, "Data (dd/mm/aaaa)")
                CampoRail(inicio, { inicio = it }, "Início (hh:mm)")
                CampoRail(fim, { fim = it }, "Fim (hh:mm)")
                CampoRail(produto, { produto = it }, "Produto")
                CampoRail(resumo, { resumo = it }, "Vagões e tonelagem (ex.: 140 vagões HPT • 14.000 t)")
                CampoRail(origem, { origem = it }, "Terminal de origem")
                CampoRail(destino, { destino = it }, "Terminal de destino (moega)")
                CampoSelecao(
                    rotulo = "Status",
                    valor = status,
                    opcoes = OPCOES_STATUS,
                    aoSelecionar = { status = OPCOES_STATUS[it] }
                )
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    locomotiva.isBlank() -> erro = "Selecione a locomotiva."
                    !RailRules.dataValida(data) -> erro = "Data inválida. Use dd/mm/aaaa."
                    !RailRules.faixaValida(inicio, fim) -> erro = "Faixa horária inválida. Use hh:mm com início anterior ao fim."
                    listOf(operador, produto, resumo, origem, destino).any { it.isBlank() } -> erro = "Preencha todos os campos."
                    else -> {
                        aoSalvar(
                            LocomotiveArrival(
                                arrivalId = chegada?.arrivalId.orEmpty(),
                                locomotiveCode = locomotiva,
                                operatorAxis = operador.trim(),
                                dateFormatted = data.trim(),
                                startTime = inicio.trim(),
                                endTime = fim.trim(),
                                product = produto.trim(),
                                wagonSummary = resumo.trim(),
                                originTerminal = origem.trim(),
                                destinationTerminal = destino.trim(),
                                status = status
                            ),
                            chegada == null
                        )
                        aoFechar()
                    }
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
