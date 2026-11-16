package br.edu.unirv.lineup.ui.screens.port

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.BerthRequest
import br.edu.unirv.lineup.ui.components.BerthDialog
import br.edu.unirv.lineup.ui.components.CampoBusca
import br.edu.unirv.lineup.ui.components.CartaoItem
import br.edu.unirv.lineup.ui.components.ConfirmDeleteDialog
import br.edu.unirv.lineup.ui.components.EstadoVazio
import br.edu.unirv.lineup.ui.components.Etiqueta

/** RF005: cadastro de berços. */
@Composable
fun BercosScreen(
    bercos: List<Berth>,
    aoSalvar: (BerthRequest, Boolean) -> Unit,
    aoExcluir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var dialogoNovo by rememberSaveable { mutableStateOf(false) }
    var editandoId by rememberSaveable { mutableStateOf<String?>(null) }
    var excluindoId by rememberSaveable { mutableStateOf<String?>(null) }

    val q = busca.trim().lowercase()
    val filtrados = bercos.filter {
        q.isEmpty() || it.id.lowercase().contains(q) || it.name.lowercase().contains(q) ||
            it.sectorName.lowercase().contains(q) || it.category.rotulo.lowercase().contains(q)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { CampoBusca(busca, { busca = it }, "Buscar por código, nome, setor ou categoria") }
        item {
            Button(onClick = { dialogoNovo = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Novo berço")
            }
        }
        if (filtrados.isEmpty()) item { EstadoVazio("Nenhum berço encontrado.") }
        items(filtrados, key = { it.id }) { b ->
            CartaoItem(
                titulo = "${b.id}  ${b.name}",
                subtitulo = b.sectorName,
                detalhes = listOf("Calado máximo ${b.maxDraft}", "${b.schedules.size} atracação(ões) programada(s)"),
                aoEditar = { editandoId = b.id },
                aoExcluir = { excluindoId = b.id },
                etiqueta = { Etiqueta(b.category.rotulo, b.category.cor) }
            )
        }
    }

    if (dialogoNovo) {
        BerthDialog(berco = null, aoSalvar = aoSalvar, aoFechar = { dialogoNovo = false })
    }
    bercos.firstOrNull { it.id == editandoId }?.let { alvo ->
        BerthDialog(berco = alvo, aoSalvar = aoSalvar, aoFechar = { editandoId = null })
    }
    bercos.firstOrNull { it.id == excluindoId }?.let { alvo ->
        ConfirmDeleteDialog(
            titulo = "Excluir berço",
            texto = "Excluir o berço ${alvo.id} (${alvo.name}) e as ${alvo.schedules.size} atracação(ões) vinculadas?",
            aoConfirmar = {
                aoExcluir(alvo.id)
                excluindoId = null
            },
            aoCancelar = { excluindoId = null }
        )
    }
}
