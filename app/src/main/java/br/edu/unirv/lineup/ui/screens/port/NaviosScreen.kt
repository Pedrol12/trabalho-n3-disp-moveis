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
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.ui.components.CampoBusca
import br.edu.unirv.lineup.ui.components.CartaoItem
import br.edu.unirv.lineup.ui.components.ConfirmDeleteDialog
import br.edu.unirv.lineup.ui.components.EstadoVazio
import br.edu.unirv.lineup.ui.components.VesselDialog
import java.text.NumberFormat
import java.util.Locale

/** RF007: cadastro de navios (IMO). */
@Composable
fun NaviosScreen(
    navios: List<RegisteredVessel>,
    aoSalvar: (RegisteredVessel, Boolean) -> Unit,
    aoExcluir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var dialogoNovo by rememberSaveable { mutableStateOf(false) }
    var editandoImo by rememberSaveable { mutableStateOf<String?>(null) }
    var excluindoImo by rememberSaveable { mutableStateOf<String?>(null) }

    val q = busca.trim().lowercase()
    val filtrados = navios.filter {
        q.isEmpty() || it.shipName.lowercase().contains(q) || it.imoCode.lowercase().contains(q) ||
            it.vesselClass.lowercase().contains(q)
    }
    val formato = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"))

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { CampoBusca(busca, { busca = it }, "Buscar por nome, IMO ou classe") }
        item {
            Button(onClick = { dialogoNovo = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Novo navio")
            }
        }
        if (filtrados.isEmpty()) item { EstadoVazio("Nenhum navio encontrado.") }
        items(filtrados, key = { it.imoCode }) { n ->
            CartaoItem(
                titulo = n.shipName,
                subtitulo = "${n.imoCode}  |  ${n.vesselClass}",
                detalhes = listOf(
                    "Calado máximo ${n.maxDraftMeters} m  |  ${formato.format(n.defaultTonnage)} t padrão",
                    "Descarga: ${n.defaultUnloadProduct}  |  Carga: ${n.defaultLoadProduct}"
                ),
                aoEditar = { editandoImo = n.imoCode },
                aoExcluir = { excluindoImo = n.imoCode }
            )
        }
    }

    if (dialogoNovo) VesselDialog(navio = null, aoSalvar = aoSalvar, aoFechar = { dialogoNovo = false })
    navios.firstOrNull { it.imoCode == editandoImo }?.let { alvo ->
        VesselDialog(navio = alvo, aoSalvar = aoSalvar, aoFechar = { editandoImo = null })
    }
    navios.firstOrNull { it.imoCode == excluindoImo }?.let { alvo ->
        ConfirmDeleteDialog(
            titulo = "Excluir navio",
            texto = "Excluir ${alvo.shipName} (${alvo.imoCode}) do cadastro?",
            aoConfirmar = {
                aoExcluir(alvo.imoCode)
                excluindoImo = null
            },
            aoCancelar = { excluindoImo = null }
        )
    }
}
