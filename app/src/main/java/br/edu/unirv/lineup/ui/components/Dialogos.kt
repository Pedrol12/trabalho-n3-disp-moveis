package br.edu.unirv.lineup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.BerthRequest
import br.edu.unirv.lineup.data.model.CargoCategory
import br.edu.unirv.lineup.data.model.LineupRules
import br.edu.unirv.lineup.data.model.PortCargo
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.ShipScheduleRequest
import br.edu.unirv.lineup.data.model.StatusNavio
import br.edu.unirv.lineup.data.model.taxaSugerida

private val TecladoNumero = KeyboardOptions(keyboardType = KeyboardType.Number)

/**
 * Diálogo de atracação (RF004/RF009). [atracacao] nulo significa nova atracação.
 * [aoSalvar] devolve false quando a validação local reprova (o diálogo permanece aberto).
 */
@Composable
fun ShipMooringDialog(
    atracacao: ShipSchedule?,
    bercoInicialId: String,
    bercos: List<Berth>,
    navios: List<RegisteredVessel>,
    cargas: List<PortCargo>,
    diasMareBaixa: Set<Int>,
    aoSalvar: (Long?, ShipScheduleRequest) -> Boolean,
    aoExcluir: ((Long) -> Unit)?,
    aoFechar: () -> Unit
) {
    val chave = atracacao?.id ?: -999999L
    var bercoId by rememberSaveable(chave) { mutableStateOf(atracacao?.berthId ?: bercoInicialId) }
    var nome by rememberSaveable(chave) { mutableStateOf(atracacao?.shipName.orEmpty()) }
    var descarga by rememberSaveable(chave) { mutableStateOf(atracacao?.unloadingProduct.orEmpty()) }
    var carga by rememberSaveable(chave) { mutableStateOf(atracacao?.loadingProduct.orEmpty()) }
    var toneladas by rememberSaveable(chave) { mutableStateOf((atracacao?.tonnage ?: 30000).toString()) }
    var inicio by rememberSaveable(chave) { mutableStateOf(LineupRules.formatarDia(atracacao?.startDay ?: 1.0)) }
    var fim by rememberSaveable(chave) { mutableStateOf(LineupRules.formatarDia(atracacao?.endDay ?: 4.0)) }
    var status by rememberSaveable(chave) { mutableStateOf(atracacao?.status ?: StatusNavio.PROGRAMADO) }
    var aviso by rememberSaveable(chave) { mutableStateOf<String?>(null) }

    val berco = bercos.firstOrNull { it.id == bercoId }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (atracacao == null) "Atracar navio" else "Atracação de ${atracacao.shipName}") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CampoSelecao(
                    rotulo = "Berço",
                    valor = berco?.let { "${it.id} - ${it.name}" } ?: "Selecione",
                    opcoes = bercos.map { "${it.id} - ${it.name}" },
                    aoSelecionar = { bercoId = bercos[it].id }
                )
                if (navios.isNotEmpty()) {
                    CampoSelecao(
                        rotulo = "Navio cadastrado (opcional)",
                        valor = navios.firstOrNull { it.shipName == nome }?.let { "${it.shipName} (${it.imoCode})" } ?: "Escolher do cadastro",
                        opcoes = navios.map { "${it.shipName} (${it.imoCode})" },
                        aoSelecionar = {
                            val n = navios[it]
                            nome = n.shipName
                            toneladas = n.defaultTonnage.toString()
                            descarga = n.defaultUnloadProduct
                            carga = n.defaultLoadProduct
                        }
                    )
                }
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome do navio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(descarga, { descarga = it }, label = { Text("Produto de descarga") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(carga, { carga = it }, label = { Text("Produto de carga") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    toneladas, { toneladas = it.filter(Char::isDigit) },
                    label = { Text("Toneladas") }, singleLine = true,
                    keyboardOptions = TecladoNumero, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        inicio, { inicio = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Chegada (dia)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        fim, { fim = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Saída (dia)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedButton(
                    onClick = {
                        val alvo = berco
                        if (alvo == null) {
                            aviso = "Selecione um berço."
                        } else {
                            val ton = toneladas.toIntOrNull() ?: 0
                            val taxa = taxaSugerida(cargas, alvo.category, listOf(carga, descarga))
                            val dias = LineupRules.duracaoSugerida(ton, taxa)
                            val semEste = alvo.copy(schedules = alvo.schedules.filter { it.id != atracacao?.id })
                            val janela = semEste.primeiraJanelaLivre(dias, diasMareBaixa)
                            if (janela == null) {
                                aviso = "Não há janela livre no berço para $dias dia(s)."
                            } else {
                                inicio = LineupRules.formatarDia(janela.first)
                                fim = LineupRules.formatarDia(janela.second)
                                aviso = "Duração sugerida: $dias dia(s) ($ton t a $taxa t/dia). Janela livre aplicada."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sugerir janela pela taxa diária") }
                CampoSelecao(
                    rotulo = "Status",
                    valor = status,
                    opcoes = StatusNavio.todos,
                    aoSelecionar = { status = StatusNavio.todos[it] }
                )
                aviso?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ini = inicio.toDoubleOrNull()
                val fi = fim.toDoubleOrNull()
                if (ini == null || fi == null) {
                    aviso = "Informe os dias de chegada e saída."
                } else {
                    val req = ShipScheduleRequest(
                        berthId = bercoId,
                        shipName = nome.trim(),
                        unloadingProduct = descarga.trim().ifBlank { null },
                        loadingProduct = carga.trim().ifBlank { null },
                        tonnage = toneladas.toIntOrNull() ?: 0,
                        startDay = ini,
                        endDay = fi,
                        stripeColorHex = StatusNavio.corHex(status),
                        status = status
                    )
                    if (aoSalvar(atracacao?.id, req)) aoFechar()
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (atracacao?.id != null && aoExcluir != null) {
                    TextButton(onClick = {
                        aoExcluir(atracacao.id)
                        aoFechar()
                    }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = aoFechar) { Text("Cancelar") }
            }
        }
    )
}

@Composable
fun BerthDialog(
    berco: Berth?,
    aoSalvar: (BerthRequest, Boolean) -> Unit,
    aoFechar: () -> Unit
) {
    val chave = berco?.id ?: "novo"
    var codigo by rememberSaveable(chave) { mutableStateOf(berco?.id.orEmpty()) }
    var nome by rememberSaveable(chave) { mutableStateOf(berco?.name.orEmpty()) }
    var setor by rememberSaveable(chave) { mutableStateOf(berco?.sectorName.orEmpty()) }
    var calado by rememberSaveable(chave) { mutableStateOf(berco?.maxDraft.orEmpty()) }
    var categoria by rememberSaveable(chave) { mutableStateOf((berco?.category ?: CargoCategory.MULTIPURPOSE).name) }
    var erro by rememberSaveable(chave) { mutableStateOf<String?>(null) }
    val cat = CargoCategory.valueOf(categoria)

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (berco == null) "Novo berço" else "Editar berço ${berco.id}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    codigo, { codigo = it.uppercase() }, label = { Text("Código (ex.: SV-03)") },
                    singleLine = true, enabled = berco == null, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(setor, { setor = it }, label = { Text("Setor") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(calado, { calado = it }, label = { Text("Calado máximo (ex.: 14.0m)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                CampoSelecao(
                    rotulo = "Categoria de carga",
                    valor = cat.rotulo,
                    opcoes = CargoCategory.entries.map { it.rotulo },
                    aoSelecionar = { categoria = CargoCategory.entries[it].name }
                )
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (codigo.isBlank() || nome.isBlank()) {
                    erro = "Código e nome são obrigatórios."
                } else {
                    aoSalvar(BerthRequest(codigo.trim(), nome.trim(), setor.trim(), calado.trim(), cat), berco == null)
                    aoFechar()
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}

@Composable
fun CargoDialog(
    carga: PortCargo?,
    aoSalvar: (PortCargo, Boolean) -> Unit,
    aoFechar: () -> Unit
) {
    val chave = carga?.productCode ?: "nova"
    var codigo by rememberSaveable(chave) { mutableStateOf(carga?.productCode.orEmpty()) }
    var nome by rememberSaveable(chave) { mutableStateOf(carga?.productName.orEmpty()) }
    var direcao by rememberSaveable(chave) { mutableStateOf(carga?.operationDirection?.trimStart { !it.isLetter() } ?: "Descarga") }
    var perfil by rememberSaveable(chave) { mutableStateOf((carga?.cargoProfile ?: CargoCategory.MULTIPURPOSE).name) }
    var taxa by rememberSaveable(chave) { mutableStateOf(carga?.averageRateTonsDay.orEmpty()) }
    var erro by rememberSaveable(chave) { mutableStateOf<String?>(null) }
    val direcoes = listOf("Descarga", "Carga", "Descarga & Carga")

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (carga == null) "Nova carga" else "Editar carga ${carga.productCode}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    codigo, { codigo = it.uppercase() }, label = { Text("Código (ex.: CRG-16)") },
                    singleLine = true, enabled = carga == null, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(nome, { nome = it }, label = { Text("Produto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                CampoSelecao("Operação", direcao, direcoes, { direcao = direcoes[it] })
                CampoSelecao(
                    "Perfil de carga", CargoCategory.valueOf(perfil).rotulo,
                    CargoCategory.entries.map { it.rotulo }, { perfil = CargoCategory.entries[it].name }
                )
                OutlinedTextField(taxa, { taxa = it }, label = { Text("Taxa média (ex.: 36.000 t/dia)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (codigo.isBlank() || nome.isBlank()) {
                    erro = "Código e produto são obrigatórios."
                } else {
                    aoSalvar(PortCargo(codigo.trim(), nome.trim(), direcao, CargoCategory.valueOf(perfil), taxa.trim()), carga == null)
                    aoFechar()
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}

@Composable
fun VesselDialog(
    navio: RegisteredVessel?,
    aoSalvar: (RegisteredVessel, Boolean) -> Unit,
    aoFechar: () -> Unit
) {
    val chave = navio?.imoCode ?: "novo"
    var nome by rememberSaveable(chave) { mutableStateOf(navio?.shipName.orEmpty()) }
    var classe by rememberSaveable(chave) { mutableStateOf(navio?.vesselClass.orEmpty()) }
    var calado by rememberSaveable(chave) { mutableStateOf(navio?.maxDraftMeters.orEmpty()) }
    var toneladas by rememberSaveable(chave) { mutableStateOf((navio?.defaultTonnage ?: 30000).toString()) }
    var descarga by rememberSaveable(chave) { mutableStateOf(navio?.defaultUnloadProduct.orEmpty()) }
    var carga by rememberSaveable(chave) { mutableStateOf(navio?.defaultLoadProduct.orEmpty()) }
    var erro by rememberSaveable(chave) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (navio == null) "Novo navio" else "Editar ${navio.imoCode}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome do navio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(classe, { classe = it }, label = { Text("Classe") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(calado, { calado = it }, label = { Text("Calado máximo (m)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    toneladas, { toneladas = it.filter(Char::isDigit) }, label = { Text("Toneladas padrão") },
                    singleLine = true, keyboardOptions = TecladoNumero, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(descarga, { descarga = it }, label = { Text("Produto de descarga padrão") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(carga, { carga = it }, label = { Text("Produto de carga padrão") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nome.isBlank()) {
                    erro = "Informe o nome do navio."
                } else {
                    aoSalvar(
                        RegisteredVessel(
                            navio?.imoCode.orEmpty(), nome.trim(), classe.trim(), calado.trim(),
                            toneladas.toIntOrNull() ?: 0, descarga.trim(), carga.trim()
                        ),
                        navio == null
                    )
                    aoFechar()
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
