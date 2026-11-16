package br.edu.unirv.lineup.ui.screens.rail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.data.model.RailAxisYard
import br.edu.unirv.lineup.ui.components.EstadoVazio
import br.edu.unirv.lineup.ui.components.Etiqueta
import br.edu.unirv.lineup.ui.theme.Paleta

/** RF012: eixos tronco e pátios (somente leitura). */
@Composable
fun RailAxesScreen(eixos: List<RailAxisYard>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (eixos.isEmpty()) item { EstadoVazio("Nenhum eixo ou pátio disponível.") }
        items(eixos, key = { it.code }) { e ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(e.code, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(e.name, style = MaterialTheme.typography.titleMedium)
                    Text(e.categoryType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Concessionária: ${e.concessionaire}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                    Text("Extensão / capacidade: ${e.maxTrainLengthKm}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                    Text("AMVs: ${e.switchAmvCount}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                    val operacional = e.operationalStatus.lowercase().let { it.startsWith("oper") || it.startsWith("ativo") }
                    Etiqueta(
                        e.operationalStatus,
                        if (operacional) Paleta.Online else Paleta.Offline,
                        Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
