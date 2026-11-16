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
import br.edu.unirv.lineup.data.model.PortCargo
import br.edu.unirv.lineup.ui.components.CampoBusca
import br.edu.unirv.lineup.ui.components.CargoDialog
import br.edu.unirv.lineup.ui.components.CartaoItem
import br.edu.unirv.lineup.ui.components.ConfirmDeleteDialog
import br.edu.unirv.lineup.ui.components.EstadoVazio
import br.edu.unirv.lineup.ui.components.Etiqueta

/** RF006: catálogo de cargas portuárias. */
@Composable
fun CargasScreen(
    cargas: List<PortCargo>,
    aoSalvar: (PortCargo, Boolean) -> Unit,
    aoExcluir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var dialogoNovo by rememberSaveable { mutableStateOf(false) }
    var editandoCodigo by rememberSaveable { mutableStateOf<String?>(null) }
    var excluindoCodigo by rememberSaveable { mutableStateOf<String?>(null) }

    val q = busca.trim().lowercase()
    val filtradas = cargas.filter {
        q.isEmpty() || it.productCode.lowercase().contains(q) || it.productName.lowercase().contains(q) ||
            it.cargoProfile.rotulo.lowercase().contains(q)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { CampoBusca(busca, { busca = it }, "Buscar por código, produto ou perfil") }
        item {
            Button(onClick = { dialogoNovo = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nova carga")
            }
        }
        if (filtradas.isEmpty()) item { EstadoVazio("Nenhuma carga encontrada.") }
        items(filtradas, key = { it.productCode }) { c ->
            CartaoItem(
                titulo = c.productName,
                subtitulo = c.productCode,
                detalhes = listOf(
                    "Operação: ${c.operationDirection.trimStart { !it.isLetter() }}",
                    "Taxa média: ${c.averageRateTonsDay}"
                ),
                aoEditar = { editandoCodigo = c.productCode },
                aoExcluir = { excluindoCodigo = c.productCode },
                etiqueta = { Etiqueta(c.cargoProfile.rotulo, c.cargoProfile.cor) }
            )
        }
    }

    if (dialogoNovo) CargoDialog(carga = null, aoSalvar = aoSalvar, aoFechar = { dialogoNovo = false })
    cargas.firstOrNull { it.productCode == editandoCodigo }?.let { alvo ->
        CargoDialog(carga = alvo, aoSalvar = aoSalvar, aoFechar = { editandoCodigo = null })
    }
    cargas.firstOrNull { it.productCode == excluindoCodigo }?.let { alvo ->
        ConfirmDeleteDialog(
            titulo = "Excluir carga",
            texto = "Excluir ${alvo.productName} (${alvo.productCode}) do catálogo?",
            aoConfirmar = {
                aoExcluir(alvo.productCode)
                excluindoCodigo = null
            },
            aoCancelar = { excluindoCodigo = null }
        )
    }
}
