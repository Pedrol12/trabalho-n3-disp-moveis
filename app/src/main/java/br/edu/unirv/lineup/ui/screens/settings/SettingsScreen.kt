package br.edu.unirv.lineup.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.ResultadoConexao
import br.edu.unirv.lineup.data.remote.BackendConfig
import br.edu.unirv.lineup.ui.theme.Paleta

/** RF002: URL do backend, presets e teste de conexão. */
@Composable
fun SettingsScreen(
    urlAtual: String,
    resultado: ResultadoConexao?,
    testando: Boolean,
    aoSalvarETestar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by rememberSaveable { mutableStateOf(urlAtual) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Endereço do LineupBackend", style = MaterialTheme.typography.titleMedium)
        Text(
            "Informe a URL base do servidor Spring Boot. No emulador use 10.0.2.2; em aparelho físico use o IP " +
                "do computador na mesma rede Wi-Fi (ou adb reverse tcp:8080 tcp:8080 com a URL local).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL base") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { url = BackendConfig.DEFAULT_NETWORK_URL }, modifier = Modifier.weight(1f)) {
                Text("Wi-Fi")
            }
            OutlinedButton(onClick = { url = BackendConfig.EMULATOR_URL }, modifier = Modifier.weight(1f)) {
                Text("Emulador")
            }
            OutlinedButton(onClick = { url = BackendConfig.LOCAL_URL }, modifier = Modifier.weight(1f)) {
                Text("Local")
            }
        }
        Button(onClick = { aoSalvarETestar(url) }, enabled = !testando, modifier = Modifier.fillMaxWidth()) {
            Text(if (testando) "Testando..." else "Salvar e Testar Conexão")
        }
        resultado?.let {
            val cor = if (it.sucesso) Paleta.Online else Paleta.Erro
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = cor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, cor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    it.texto,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cor
                )
            }
        }
    }
}
