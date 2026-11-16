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
import androidx.compose.material3.OutlinedButton
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
import br.edu.unirv.lineup.data.model.LocomotiveRequest
import br.edu.unirv.lineup.data.model.Wagon
import br.edu.unirv.lineup.data.model.WagonRequest
import br.edu.unirv.lineup.ui.components.CampoBusca
import br.edu.unirv.lineup.ui.components.CartaoItem
import br.edu.unirv.lineup.ui.components.ConfirmDeleteDialog
import br.edu.unirv.lineup.ui.components.EstadoVazio

/** Potência em HP extraída de "Wabtec ES44ACi • 4.500 HP"; null se o texto não trouxer HP. */
fun potenciaHp(modelPower: String): String? =
    Regex("""([\d.,]+)\s*HP""", RegexOption.IGNORE_CASE).find(modelPower)?.groupValues?.get(1)?.let { "$it HP" }

/** RF013: CRUD de locomotivas. O botão "Vagões (N)" abre o modal RF014 já selecionado nela. */
@Composable
fun RailLocomotivesScreen(
    locomotivas: List<Locomotive>,
    vagoes: List<Wagon>,
    aoSalvar: (LocomotiveRequest, Boolean) -> Unit,
    aoExcluir: (String) -> Unit,
    aoSalvarVagao: (WagonRequest, Boolean) -> Unit,
    aoExcluirVagao: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var dialogoNovo by rememberSaveable { mutableStateOf(false) }
    var editando by rememberSaveable { mutableStateOf<String?>(null) }
    var excluindo by rememberSaveable { mutableStateOf<String?>(null) }
    var modalAberto by rememberSaveable { mutableStateOf(false) }
    var modalLocomotiva by rememberSaveable { mutableStateOf<String?>(null) }

    val q = busca.trim().lowercase()
    val filtradas = locomotivas.filter {
        q.isEmpty() || it.locomotiveCode.lowercase().contains(q) || it.modelPower.lowercase().contains(q) ||
            it.operatorAxis.lowercase().contains(q)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { CampoBusca(busca, { busca = it }, "Buscar por código, modelo ou operador") }
        item {
            Button(onClick = { dialogoNovo = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nova locomotiva")
            }
        }
        if (filtradas.isEmpty()) item { EstadoVazio("Nenhuma locomotiva encontrada.") }
        items(filtradas, key = { it.locomotiveCode }) { l ->
            val total = vagoes.count { it.locomotiveCode == l.locomotiveCode }
            CartaoItem(
                titulo = l.locomotiveCode,
                subtitulo = l.modelPower,
                detalhes = listOfNotNull(
                    potenciaHp(l.modelPower)?.let { "Potência: $it" },
                    "Operador: ${l.operatorAxis}",
                    "Vagões engatados: $total"
                ),
                aoEditar = { editando = l.locomotiveCode },
                aoExcluir = { excluindo = l.locomotiveCode },
                etiqueta = {
                    OutlinedButton(onClick = {
                        modalLocomotiva = l.locomotiveCode
                        modalAberto = true
                    }) { Text("Vagões ($total)") }
                }
            )
        }
    }

    if (dialogoNovo) LocomotiveDialog(null, aoSalvar) { dialogoNovo = false }
    locomotivas.firstOrNull { it.locomotiveCode == editando }?.let { alvo ->
        LocomotiveDialog(alvo, aoSalvar) { editando = null }
    }
    locomotivas.firstOrNull { it.locomotiveCode == excluindo }?.let { alvo ->
        ConfirmDeleteDialog(
            titulo = "Excluir locomotiva",
            texto = "Excluir ${alvo.locomotiveCode}? Os vagões engatados também podem ser removidos pelo servidor.",
            aoConfirmar = {
                aoExcluir(alvo.locomotiveCode)
                excluindo = null
            },
            aoCancelar = { excluindo = null }
        )
    }
    if (modalAberto) {
        WagonsManagementModal(
            locomotivas = locomotivas,
            vagoes = vagoes,
            locomotivaInicial = modalLocomotiva,
            aoSalvarVagao = aoSalvarVagao,
            aoExcluirVagao = aoExcluirVagao,
            aoFechar = { modalAberto = false }
        )
    }
}

@Composable
private fun LocomotiveDialog(
    locomotiva: Locomotive?,
    aoSalvar: (LocomotiveRequest, Boolean) -> Unit,
    aoFechar: () -> Unit
) {
    val chave = locomotiva?.locomotiveCode ?: "nova"
    var codigo by rememberSaveable(chave) { mutableStateOf(locomotiva?.locomotiveCode.orEmpty()) }
    var modelo by rememberSaveable(chave) { mutableStateOf(locomotiva?.modelPower.orEmpty()) }
    var operador by rememberSaveable(chave) { mutableStateOf(locomotiva?.operatorAxis.orEmpty()) }
    var erro by rememberSaveable(chave) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (locomotiva == null) "Nova locomotiva" else "Editar ${locomotiva.locomotiveCode}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoRail(codigo, { codigo = it }, "Código (ex.: LOC-9042 (RUMO-MT-01))", habilitado = locomotiva == null)
                CampoRail(modelo, { modelo = it }, "Modelo e potência (ex.: GE AC44i • 4.400 HP)")
                CampoRail(operador, { operador = it }, "Operador / eixo")
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (codigo.isBlank() || modelo.isBlank() || operador.isBlank()) {
                    erro = "Preencha código, modelo e operador."
                } else {
                    aoSalvar(LocomotiveRequest(codigo.trim(), modelo.trim(), operador.trim()), locomotiva == null)
                    aoFechar()
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
