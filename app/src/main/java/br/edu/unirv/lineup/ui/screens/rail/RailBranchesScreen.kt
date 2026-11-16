package br.edu.unirv.lineup.ui.screens.rail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.data.model.RailTerminalBranch
import br.edu.unirv.lineup.ui.components.CartaoKpi
import br.edu.unirv.lineup.ui.components.EstadoVazio
import java.text.NumberFormat
import java.util.Locale

/** RF011: ramais e moegas com chave de AMV. */
@Composable
fun RailBranchesScreen(
    ramais: List<RailTerminalBranch>,
    aoAlternarAmv: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val formato = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"))
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CartaoKpi("AMVs ativos", "${ramais.count { it.switchActive }}/${ramais.size}", Modifier.weight(1f))
                CartaoKpi(
                    "Capacidade ativa",
                    "${formato.format(ramais.filter { it.switchActive }.sumOf { it.capacityCarsPerDay })} vag./dia",
                    Modifier.weight(1f)
                )
            }
        }
        if (ramais.isEmpty()) item { EstadoVazio("Nenhum ramal ou moega disponível.") }
        items(ramais, key = { it.code }) { r ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.code, style = MaterialTheme.typography.titleMedium)
                        Text(
                            r.terminalName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            "${r.region}  |  ${r.hopperType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Capacidade: ${formato.format(r.capacityCarsPerDay)} vagões/dia  |  ${r.mainCommodity}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                        Text(
                            if (r.switchActive) "AMV ativo" else "AMV desativado",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (r.switchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Switch(checked = r.switchActive, onCheckedChange = { aoAlternarAmv(r.code, it) })
                }
            }
        }
    }
}
