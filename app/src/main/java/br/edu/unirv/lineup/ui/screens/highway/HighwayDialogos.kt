package br.edu.unirv.lineup.ui.screens.highway

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import br.edu.unirv.lineup.data.model.HighwayCargo
import br.edu.unirv.lineup.data.model.HighwayLine
import br.edu.unirv.lineup.data.model.HighwayRules
import br.edu.unirv.lineup.data.model.RegisteredTruck
import br.edu.unirv.lineup.data.model.TruckSchedule
import br.edu.unirv.lineup.ui.components.CampoSelecao

@Composable
fun CampoRodovia(
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

private val MINUTOS = listOf(0, 15, 30, 45)

/**
 * RF017/RF022: diálogo de agendamento de caminhão. [agendamento] nulo significa novo.
 * Valida localmente RN016 (início e duração em quartos de hora, por construção) e RN017 (HighwayLine.hasOverlap).
 */
@Composable
fun TruckAppointmentDialog(
    agendamento: TruckSchedule?,
    docaInicial: Int,
    diaInicial: Int,
    docas: List<HighwayLine>,
    cadastrados: List<RegisteredTruck>,
    aoSalvar: (Int, TruckSchedule) -> Boolean,
    aoExcluir: ((String) -> Unit)?,
    aoFechar: () -> Unit
) {
    val chave = agendamento?.truckId ?: "novo"
    val partes = HighwayRules.separarPlacaFrota(agendamento?.plateAndFleet.orEmpty())
    val slotIni = agendamento?.startSlot ?: HighwayRules.slotDe(diaInicial, 8, 0)
    val duracaoIni = agendamento?.let { it.endSlot - it.startSlot } ?: 12

    var doca by rememberSaveable(chave) { mutableStateOf((agendamento?.lineNumber ?: docaInicial).toString()) }
    var placa by rememberSaveable(chave) { mutableStateOf(partes.first) }
    var frota by rememberSaveable(chave) { mutableStateOf(partes.second) }
    var transportadora by rememberSaveable(chave) { mutableStateOf(agendamento?.carrierName.orEmpty()) }
    var descarga by rememberSaveable(chave) { mutableStateOf(agendamento?.unloadingProduct.orEmpty()) }
    var retorno by rememberSaveable(chave) { mutableStateOf(agendamento?.loadingProduct.orEmpty()) }
    var toneladas by rememberSaveable(chave) { mutableStateOf((agendamento?.tonnage ?: 74.0).toString()) }
    var dia by rememberSaveable(chave) { mutableStateOf(HighwayRules.diaDoSlot(slotIni)) }
    var hora by rememberSaveable(chave) { mutableStateOf(HighwayRules.horaDoSlot(slotIni)) }
    var minuto by rememberSaveable(chave) { mutableStateOf(HighwayRules.minutoDoSlot(slotIni)) }
    var duracao by rememberSaveable(chave) { mutableStateOf(duracaoIni.toString()) }
    var erro by rememberSaveable(chave) { mutableStateOf<String?>(null) }

    val inicio = HighwayRules.slotDe(dia, hora, minuto)
    val slots = duracao.trim().toIntOrNull()
    val fim = slots?.let { inicio + it }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (agendamento == null) "Novo agendamento" else "Agendamento ${agendamento.plateAndFleet}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoRodovia(doca, { doca = it.filter(Char::isDigit).take(3) }, "Doca (1 a ${HighwayRules.DOCAS})", numerico = true)
                if (cadastrados.isNotEmpty()) {
                    CampoSelecao(
                        rotulo = "Preencher com caminhão cadastrado",
                        valor = "Selecione (opcional)",
                        opcoes = cadastrados.map { "${it.truckCode} - ${it.plateAndFleet}" },
                        aoSelecionar = { i ->
                            val c = cadastrados[i]
                            val (p, f) = HighwayRules.separarPlacaFrota(c.plateAndFleet)
                            placa = p; frota = f
                            transportadora = c.carrierName
                            descarga = c.defaultUnloadProduct
                            retorno = c.defaultLoadProduct
                            toneladas = c.maxCapacityTons.toString()
                        }
                    )
                }
                CampoRodovia(placa, { placa = it.uppercase() }, "Placa (ex.: KLM-7N45)")
                CampoRodovia(frota, { frota = it }, "Código de frota (ex.: 108)")
                CampoRodovia(transportadora, { transportadora = it }, "Transportadora")
                CampoRodovia(descarga, { descarga = it }, "Produto de descarga")
                CampoRodovia(retorno, { retorno = it }, "Produto de retorno")
                CampoRodovia(toneladas, { toneladas = it }, "Tonelagem (t)")
                CampoSelecao(
                    rotulo = "Dia de chegada",
                    valor = "Dia %02d".format(dia),
                    opcoes = (1..HighwayRules.DIAS).map { "Dia %02d".format(it) },
                    aoSelecionar = { dia = it + 1 }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CampoSelecao(
                        rotulo = "Hora",
                        valor = "%02d".format(hora),
                        opcoes = (0..23).map { "%02d".format(it) },
                        aoSelecionar = { hora = it },
                        modifier = Modifier.weight(1f)
                    )
                    CampoSelecao(
                        rotulo = "Minuto",
                        valor = "%02d".format(minuto),
                        opcoes = MINUTOS.map { "%02d".format(it) },
                        aoSelecionar = { minuto = MINUTOS[it] },
                        modifier = Modifier.weight(1f)
                    )
                }
                CampoRodovia(duracao, { duracao = it.filter(Char::isDigit).take(4) }, "Duração em slots de 15 min", numerico = true)
                Text(
                    if (fim != null && fim > inicio)
                        "Saída: ${HighwayRules.formatarSlot(fim)} (${slots * 15} min)"
                    else "Informe uma duração de 1 slot ou mais.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val numeroDoca = doca.toIntOrNull()
                val alvo = docas.firstOrNull { it.lineNumber == numeroDoca }
                val t = toneladas.trim().replace(',', '.').toDoubleOrNull()
                when {
                    alvo == null -> erro = "Doca inexistente. Use um número de 1 a ${HighwayRules.DOCAS}."
                    !HighwayRules.placaValida(placa) -> erro = "Placa inválida. Use o padrão Mercosul (AAA-0A00) ou antigo (AAA-0000)."
                    frota.isBlank() -> erro = "Informe o código de frota."
                    transportadora.isBlank() || descarga.isBlank() || retorno.isBlank() ->
                        erro = "Preencha transportadora e os produtos de descarga e retorno."
                    t == null || t <= 0.0 -> erro = "Tonelagem inválida."
                    fim == null || fim <= inicio -> erro = "Informe uma duração de 1 slot ou mais."
                    else -> {
                        val janela = HighwayRules.validarJanela(alvo, inicio, fim, agendamento?.truckId)
                        if (janela != null) {
                            erro = janela
                        } else {
                            val salvo = TruckSchedule(
                                truckId = agendamento?.truckId.orEmpty(),
                                plateAndFleet = HighwayRules.montarPlacaFrota(HighwayRules.normalizarPlaca(placa), frota),
                                carrierName = transportadora.trim(),
                                unloadingProduct = descarga.trim(),
                                loadingProduct = retorno.trim(),
                                tonnage = t,
                                startSlot = inicio,
                                endSlot = fim,
                                colorHex = agendamento?.colorHex.orEmpty(),
                                lineNumber = alvo.lineNumber
                            )
                            if (aoSalvar(alvo.lineNumber, salvo)) aoFechar()
                        }
                    }
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (agendamento != null && aoExcluir != null) {
                    TextButton(onClick = {
                        aoExcluir(agendamento.truckId)
                        aoFechar()
                    }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = aoFechar) { Text("Cancelar") }
            }
        }
    )
}

/** RF020: cadastro de caminhão (placa Mercosul + frota + configuração/PBTC). */
@Composable
fun RegisteredTruckDialog(
    caminhao: RegisteredTruck?,
    aoSalvar: (RegisteredTruck, Boolean) -> Unit,
    aoFechar: () -> Unit
) {
    val chave = caminhao?.truckCode ?: "novo"
    val partes = HighwayRules.separarPlacaFrota(caminhao?.plateAndFleet.orEmpty())
    var codigo by rememberSaveable(chave) { mutableStateOf(caminhao?.truckCode.orEmpty()) }
    var placa by rememberSaveable(chave) { mutableStateOf(partes.first) }
    var frota by rememberSaveable(chave) { mutableStateOf(partes.second) }
    var transportadora by rememberSaveable(chave) { mutableStateOf(caminhao?.carrierName.orEmpty()) }
    var configuracao by rememberSaveable(chave) { mutableStateOf(caminhao?.truckConfigType.orEmpty()) }
    var capacidade by rememberSaveable(chave) { mutableStateOf((caminhao?.maxCapacityTons ?: 74.0).toString()) }
    var descarga by rememberSaveable(chave) { mutableStateOf(caminhao?.defaultUnloadProduct.orEmpty()) }
    var retorno by rememberSaveable(chave) { mutableStateOf(caminhao?.defaultLoadProduct.orEmpty()) }
    var erro by rememberSaveable(chave) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (caminhao == null) "Novo caminhão" else "Caminhão ${caminhao.truckCode}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoRodovia(codigo, { codigo = it.uppercase() }, "Código (ex.: FRT-031)", habilitado = caminhao == null)
                CampoRodovia(placa, { placa = it.uppercase() }, "Placa Mercosul (ex.: KLM-7N45)")
                CampoRodovia(frota, { frota = it }, "Código de frota (ex.: 108)")
                CampoRodovia(transportadora, { transportadora = it }, "Transportadora")
                CampoRodovia(configuracao, { configuracao = it }, "Configuração / PBTC (ex.: Rodotrem 74.0t)")
                CampoRodovia(capacidade, { capacidade = it }, "Capacidade máxima (t)")
                CampoRodovia(descarga, { descarga = it }, "Produto de descarga padrão (sentido ↓)")
                CampoRodovia(retorno, { retorno = it }, "Produto de retorno padrão (sentido ↑)")
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cap = capacidade.trim().replace(',', '.').toDoubleOrNull()
                when {
                    codigo.isBlank() -> erro = "Informe o código do caminhão."
                    !HighwayRules.placaValida(placa) -> erro = "Placa inválida. Use o padrão Mercosul (AAA-0A00) ou antigo (AAA-0000)."
                    frota.isBlank() -> erro = "Informe o código de frota."
                    listOf(transportadora, configuracao, descarga, retorno).any { it.isBlank() } -> erro = "Preencha todos os campos."
                    cap == null || cap <= 0.0 -> erro = "Capacidade inválida."
                    else -> {
                        aoSalvar(
                            RegisteredTruck(
                                truckCode = codigo.trim(),
                                plateAndFleet = HighwayRules.montarPlacaFrota(HighwayRules.normalizarPlaca(placa), frota),
                                carrierName = transportadora.trim(),
                                truckConfigType = configuracao.trim(),
                                maxCapacityTons = cap,
                                defaultUnloadProduct = descarga.trim(),
                                defaultLoadProduct = retorno.trim()
                            ),
                            caminhao == null
                        )
                        aoFechar()
                    }
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}

private const val DIRECAO_DESCARGA = "⬇ Descarga no Porto"
private const val DIRECAO_RETORNO = "⬆ Carga / Retorno Interior"

/** RF021: cadastro de carga rodoviária (descarga ↓ ou retorno ↑). */
@Composable
fun HighwayCargoDialog(
    carga: HighwayCargo?,
    aoSalvar: (HighwayCargo, Boolean) -> Unit,
    aoFechar: () -> Unit
) {
    val chave = carga?.cargoCode ?: "nova"
    var codigo by rememberSaveable(chave) { mutableStateOf(carga?.cargoCode.orEmpty()) }
    var produto by rememberSaveable(chave) { mutableStateOf(carga?.productName.orEmpty()) }
    var retorno by rememberSaveable(chave) { mutableStateOf(carga?.ehRetorno ?: false) }
    var equipamento by rememberSaveable(chave) { mutableStateOf(carga?.equipmentType.orEmpty()) }
    var slots by rememberSaveable(chave) {
        mutableStateOf((carga?.let { HighwayRules.slotsDaPermanencia(it.defaultStayDuration) } ?: 12).toString())
    }
    var erro by rememberSaveable(chave) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (carga == null) "Nova carga rodoviária" else "Carga ${carga.cargoCode}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoRodovia(codigo, { codigo = it.uppercase() }, "Código (ex.: RDC-12)", habilitado = carga == null)
                CampoRodovia(produto, { produto = it }, "Produto")
                CampoSelecao(
                    rotulo = "Operação",
                    valor = if (retorno) DIRECAO_RETORNO else DIRECAO_DESCARGA,
                    opcoes = listOf(DIRECAO_DESCARGA, DIRECAO_RETORNO),
                    aoSelecionar = { retorno = it == 1 }
                )
                CampoRodovia(equipamento, { equipamento = it }, "Equipamento (tombador, moega, guindaste...)")
                CampoRodovia(
                    slots, { slots = it.filter(Char::isDigit).take(3) }, "Permanência em slots de 15 min", numerico = true
                )
                if (carga != null) {
                    Text(
                        "Valor atual no backend: ${carga.defaultStayDuration}. Ao salvar, o texto é regravado a partir dos slots.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = slots.toIntOrNull()
                when {
                    codigo.isBlank() -> erro = "Informe o código da carga."
                    produto.isBlank() || equipamento.isBlank() -> erro = "Preencha produto e equipamento."
                    n == null || n < 1 -> erro = "Informe a permanência com 1 slot ou mais."
                    else -> {
                        aoSalvar(
                            HighwayCargo(
                                cargoCode = codigo.trim(),
                                productName = produto.trim(),
                                operationDirection = if (retorno) DIRECAO_RETORNO else DIRECAO_DESCARGA,
                                equipmentType = equipamento.trim(),
                                defaultStayDuration = HighwayRules.formatarPermanencia(n)
                            ),
                            carga == null
                        )
                        aoFechar()
                    }
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
