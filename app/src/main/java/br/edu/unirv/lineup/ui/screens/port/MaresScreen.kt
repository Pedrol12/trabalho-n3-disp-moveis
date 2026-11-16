package br.edu.unirv.lineup.ui.screens.port

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.data.model.TideTable
import br.edu.unirv.lineup.ui.components.EstadoVazio
import br.edu.unirv.lineup.ui.components.Etiqueta
import br.edu.unirv.lineup.ui.theme.Paleta

/** RF008: seletor de mês e lista dos 30 dias com mínima, máxima e situação (Liberado/Bloqueado). */
@Composable
fun MaresScreen(
    mares: List<TideTable>,
    mesSelecionadoId: Long?,
    aoSelecionarMes: (Long?) -> Unit,
    aoAtivar: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val selecionada = mares.firstOrNull { it.id == mesSelecionadoId } ?: mares.firstOrNull()
    val resumos = remember(selecionada) {
        selecionada?.let { t -> (1..30).mapNotNull { t.resumoDoDia(it) } }.orEmpty()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mares, key = { it.id ?: it.monthKey.hashCode().toLong() }) { t ->
                    FilterChip(
                        selected = t.id == selecionada?.id,
                        onClick = { aoSelecionarMes(t.id) },
                        label = { Text(if (t.activeMonth) "${t.monthLabel} (ativa)" else t.monthLabel) }
                    )
                }
            }
        }
        if (selecionada != null) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(selecionada.monthLabel, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${resumos.count { !it.liberado }} dia(s) bloqueado(s) por maré baixa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!selecionada.activeMonth) {
                        Button(onClick = { selecionada.id?.let(aoAtivar) }) { Text("Definir como ativa") }
                    }
                }
            }
        }
        if (resumos.isEmpty()) item { EstadoVazio("Sem leituras de maré disponíveis.") }
        items(resumos, key = { it.dia }) { r ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dia %02d".format(r.dia), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(0.8f))
                    Column(Modifier.weight(1.4f)) {
                        Text("Mín %.2f m".format(r.minimo), style = MaterialTheme.typography.bodyMedium)
                        Text("Máx %.2f m".format(r.maximo), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (r.liberado) Etiqueta("Liberado", Paleta.Online) else Etiqueta("Bloqueado", Paleta.Erro)
                }
            }
        }
    }
}
