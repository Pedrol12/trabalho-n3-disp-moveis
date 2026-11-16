package br.edu.unirv.lineup.ui.screens.rail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import br.edu.unirv.lineup.data.model.RailRules
import br.edu.unirv.lineup.ui.theme.Paleta

/** Converte "#RRGGBB" em Color; devolve o aço da paleta se o texto for inválido. */
fun corDeHex(hex: String): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: IllegalArgumentException) {
        Paleta.Aco
    }

/** Cor da etiqueta de status das chegadas (RF016). */
fun corDoStatus(status: String): Color = when (RailRules.categoriaDeStatus(status)) {
    RailRules.STATUS_CONCLUIDA -> Paleta.Online
    RailRules.STATUS_OCUPANDO -> Paleta.Erro
    RailRules.STATUS_APROXIMACAO -> Paleta.Offline
    else -> Paleta.Aco
}

@Composable
fun CampoRail(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    numerico: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        enabled = habilitado,
        keyboardOptions = if (numerico) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = modifier.fillMaxWidth()
    )
}
