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
import br.edu.unirv.lineup.data.model.LineupRules
import br.edu.unirv.lineup.data.model.PortCargo
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.ShipScheduleRequest
import br.edu.unirv.lineup.data.model.StatusNavio
import br.edu.unirv.lineup.data.model.corDeHex
import br.edu.unirv.lineup.ui.components.CampoBusca
import br.edu.unirv.lineup.ui.components.CartaoItem
import br.edu.unirv.lineup.ui.components.ConfirmDeleteDialog
import br.edu.unirv.lineup.ui.components.EstadoVazio
import br.edu.unirv.lineup.ui.components.Etiqueta
import br.edu.unirv.lineup.ui.components.ShipMooringDialog
import java.text.NumberFormat
import java.util.Locale

/** RF009: lista plana de todas as atracações (mesma fonte de dados do Lineup). */
@Composable
fun AtracacoesScreen(
    atracacoes: List<ShipSchedule>,
    bercos: List<Berth>,
    navios: List<RegisteredVessel>,
    cargas: List<PortCargo>,
    diasMareBaixa: Set<Int>,
    aoSalvar: (Long?, ShipScheduleRequest) -> Boolean,
    aoExcluir: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var dialogoNovo by rememberSaveable { mutableStateOf(false) }
    var editandoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var excluindoId by rememberSaveable { mutableStateOf<Long?>(null) }

    val q = busca.trim().lowercase()
    val filtradas = atracacoes.filter {
        q.isEmpty() || it.shipName.lowercase().contains(q) || it.berthId.lowercase().contains(q) ||
            it.berthName.lowercase().contains(q) || it.status.lowercase().contains(q) ||
            it.unloadingProduct.lowercase().contains(q) || it.loadingProduct.lowercase().contains(q)
    }.sortedWith(compareBy({ it.berthId }, { it.startDay }))
    val formato = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"))

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { CampoBusca(busca, { busca = it }, "Buscar por navio, berço, produto ou status") }
        item {
            Button(
                onClick = { dialogoNovo = true },
                enabled = bercos.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nova atracação")
            }
        }
        if (filtradas.isEmpty()) item { EstadoVazio("Nenhuma atracação encontrada.") }
        items(filtradas, key = { it.id ?: it.hashCode().toLong() }) { a ->
            CartaoItem(
                titulo = a.shipName,
                subtitulo = "${a.berthId}  ${a.berthName}",
                detalhes = listOf(
                    "Dias ${LineupRules.formatarDia(a.startDay)} a ${LineupRules.formatarDia(a.endDay)}  |  ${formato.format(a.tonnage)} t",
                    "Descarga: ${a.unloadingProduct}  |  Carga: ${a.loadingProduct}"
                ),
                aoEditar = { editandoId = a.id },
                aoExcluir = { excluindoId = a.id },
                etiqueta = {
                    Etiqueta(a.status, corDeHex(a.stripeColorHex.ifBlank { StatusNavio.corHex(a.status) }))
                }
            )
        }
    }

    if (dialogoNovo) {
        ShipMooringDialog(
            atracacao = null,
            bercoInicialId = bercos.firstOrNull()?.id.orEmpty(),
            bercos = bercos, navios = navios, cargas = cargas, diasMareBaixa = diasMareBaixa,
            aoSalvar = aoSalvar, aoExcluir = null, aoFechar = { dialogoNovo = false }
        )
    }
    atracacoes.firstOrNull { it.id != null && it.id == editandoId }?.let { alvo ->
        ShipMooringDialog(
            atracacao = alvo,
            bercoInicialId = alvo.berthId,
            bercos = bercos, navios = navios, cargas = cargas, diasMareBaixa = diasMareBaixa,
            aoSalvar = aoSalvar, aoExcluir = aoExcluir, aoFechar = { editandoId = null }
        )
    }
    atracacoes.firstOrNull { it.id != null && it.id == excluindoId }?.let { alvo ->
        ConfirmDeleteDialog(
            titulo = "Excluir atracação",
            texto = "Excluir a atracação de ${alvo.shipName} no berço ${alvo.berthId}?",
            aoConfirmar = {
                alvo.id?.let(aoExcluir)
                excluindoId = null
            },
            aoCancelar = { excluindoId = null }
        )
    }
}
