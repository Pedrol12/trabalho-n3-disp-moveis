package br.edu.unirv.lineup

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.edu.unirv.lineup.data.local.SantosPortDefaultData
import br.edu.unirv.lineup.data.local.TideCsvParser
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.BerthRequest
import br.edu.unirv.lineup.data.model.CargoCategory
import br.edu.unirv.lineup.data.model.HighwayCargo
import br.edu.unirv.lineup.data.model.HighwayLine
import br.edu.unirv.lineup.data.model.HighwayRules
import br.edu.unirv.lineup.data.model.HighwaySector
import br.edu.unirv.lineup.data.model.LineupRules
import br.edu.unirv.lineup.data.model.Locomotive
import br.edu.unirv.lineup.data.model.LocomotiveArrival
import br.edu.unirv.lineup.data.model.LocomotiveRequest
import br.edu.unirv.lineup.data.model.PeraHourlyState
import br.edu.unirv.lineup.data.model.PortCargo
import br.edu.unirv.lineup.data.model.RailAxisYard
import br.edu.unirv.lineup.data.model.RailRules
import br.edu.unirv.lineup.data.model.RailTerminalBranch
import br.edu.unirv.lineup.data.model.TrainHourlySnapshot
import br.edu.unirv.lineup.data.model.Wagon
import br.edu.unirv.lineup.data.model.WagonAllocation
import br.edu.unirv.lineup.data.model.WagonRequest
import br.edu.unirv.lineup.data.model.RegisteredTruck
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.ShipScheduleRequest
import br.edu.unirv.lineup.data.model.ShipScheduleWindowUpdateRequest
import br.edu.unirv.lineup.data.model.StatusNavio
import br.edu.unirv.lineup.data.model.TideTable
import br.edu.unirv.lineup.data.model.TruckSchedule
import br.edu.unirv.lineup.data.model.TruckScheduleRequest
import br.edu.unirv.lineup.data.remote.ApiException
import br.edu.unirv.lineup.data.remote.BackendApiClient
import br.edu.unirv.lineup.data.remote.BackendConfig
import br.edu.unirv.lineup.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

data class ResultadoConexao(val sucesso: Boolean, val texto: String)

data class KpisLineup(val bercos: Int, val navios: Int, val toneladas: Long)

/**
 * Único ViewModel do app. Guarda todo o estado (sobrevive à rotação) e é o único ponto que fala com
 * o BackendApiClient; as telas Compose só recebem estado e lambdas (state hoisting).
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TAMANHO_PAGINA = 5
    }

    // ------------------------------------------------------------------ estado geral

    var telaAtual by mutableStateOf<Screen>(Screen.Lineup)
        private set
    var urlBackend by mutableStateOf(BackendConfig.carregarUrl(application))
        private set
    var online by mutableStateOf(false)
        private set
    var carregando by mutableStateOf(false)
        private set
    var mensagem by mutableStateOf<String?>(null)
        private set
    var resultadoConexao by mutableStateOf<ResultadoConexao?>(null)
        private set

    // ------------------------------------------------------------------ dados do porto

    val bercos = mutableStateListOf<Berth>()
    val navios = mutableStateListOf<RegisteredVessel>()
    val cargas = mutableStateListOf<PortCargo>()
    val mares = mutableStateListOf<TideTable>()

    // ------------------------------------------------------------------ dados da ferrovia

    var peraData by mutableStateOf(LocalDate.now().toString())
        private set
    var peraHora by mutableIntStateOf(8)
        private set
    var peraEstado by mutableStateOf<PeraHourlyState?>(null)
        private set
    val dailyTrains = mutableStateListOf<TrainHourlySnapshot>()
    val railBranches = mutableStateListOf<RailTerminalBranch>()
    val railAxesYards = mutableStateListOf<RailAxisYard>()
    val locomotives = mutableStateListOf<Locomotive>()
    val wagons = mutableStateListOf<Wagon>()
    val allocations = mutableStateListOf<WagonAllocation>()
    val railArrivals = mutableStateListOf<LocomotiveArrival>()
    private var jobPera: Job? = null

    // ------------------------------------------------------------------ dados da rodovia

    val highwayLines = mutableStateListOf<HighwayLine>()
    val highwaySectors = mutableStateListOf<HighwaySector>()
    val registeredTrucks = mutableStateListOf<RegisteredTruck>()
    val highwayCargos = mutableStateListOf<HighwayCargo>()
    private var contadorIdTemporario = 0

    // ------------------------------------------------------------------ filtros / paginação

    var busca by mutableStateOf("")
        private set
    val categoriasSelecionadas = mutableStateListOf<CargoCategory>()
    var pagina by mutableIntStateOf(1)
        private set
    var mesSelecionadoId by mutableStateOf<Long?>(null)
        private set

    // ------------------------------------------------------------------ derivados

    val mareAtiva: TideTable? by derivedStateOf { mares.firstOrNull { it.activeMonth } }

    val diasMareBaixa: Set<Int> by derivedStateOf { mareAtiva?.diasMareBaixa() ?: emptySet() }

    val bercosFiltrados: List<Berth> by derivedStateOf {
        val q = busca.trim().lowercase()
        bercos.filter { b ->
            (categoriasSelecionadas.isEmpty() || b.category in categoriasSelecionadas) &&
                (q.isEmpty() || correspondeABusca(b, q))
        }
    }

    val totalPaginas: Int by derivedStateOf {
        maxOf(1, (bercosFiltrados.size + TAMANHO_PAGINA - 1) / TAMANHO_PAGINA)
    }

    val bercosDaPagina: List<Berth> by derivedStateOf {
        val p = pagina.coerceIn(1, totalPaginas)
        bercosFiltrados.drop((p - 1) * TAMANHO_PAGINA).take(TAMANHO_PAGINA)
    }

    val kpis: KpisLineup by derivedStateOf {
        val navs = bercosFiltrados.flatMap { it.schedules }
        KpisLineup(bercosFiltrados.size, navs.size, navs.sumOf { it.tonnage.toLong() })
    }

    val todasAtracacoes: List<ShipSchedule> by derivedStateOf {
        bercos.flatMap { b -> b.schedules.map { it.copy(berthId = b.id, berthName = b.name) } }
    }

    private fun correspondeABusca(b: Berth, q: String): Boolean =
        b.id.lowercase().contains(q) || b.name.lowercase().contains(q) ||
            b.sectorName.lowercase().contains(q) || b.category.rotulo.lowercase().contains(q) ||
            b.schedules.any {
                it.shipName.lowercase().contains(q) || it.status.lowercase().contains(q) ||
                    it.unloadingProduct.lowercase().contains(q) || it.loadingProduct.lowercase().contains(q)
            }

    init {
        BackendApiClient.baseUrl = urlBackend
        loadAllData()
    }

    // ------------------------------------------------------------------ navegação e UI

    fun navegarPara(tela: Screen) {
        telaAtual = tela
    }

    fun consumirMensagem() {
        mensagem = null
    }

    fun mostrarMensagem(texto: String) {
        mensagem = texto
    }

    fun alterarBusca(texto: String) {
        busca = texto
        pagina = 1
    }

    fun alternarCategoria(categoria: CargoCategory) {
        if (categoria in categoriasSelecionadas) categoriasSelecionadas.remove(categoria)
        else categoriasSelecionadas.add(categoria)
        pagina = 1
    }

    fun limparCategorias() {
        categoriasSelecionadas.clear()
        pagina = 1
    }

    fun irParaPagina(nova: Int) {
        pagina = nova.coerceIn(1, totalPaginas)
    }

    fun selecionarMes(id: Long?) {
        mesSelecionadoId = id
    }

    // ------------------------------------------------------------------ carga de dados (RF001)

    fun loadAllData() {
        viewModelScope.launch {
            carregando = true
            try {
                val ok = carregarDoServidor()
                if (!ok) {
                    mensagem = "Sem resposta do backend em $urlBackend. Usando dados locais" +
                        if (locomotives.isEmpty() && railBranches.isEmpty()) " (a Ferrovia não tem dados offline)." else "."
                }
            } finally {
                carregando = false
            }
        }
    }

    /** Busca tudo no servidor. Em caso de falha marca offline e, se preciso, usa os dados padrão. */
    private suspend fun carregarDoServidor(): Boolean {
        BackendApiClient.baseUrl = urlBackend
        try {
            val dados = withContext(Dispatchers.IO) {
                coroutineScope {
                    val b = async { BackendApiClient.listarBercos() }
                    val n = async { BackendApiClient.listarNavios() }
                    val c = async { BackendApiClient.listarCargas() }
                    val m = async { BackendApiClient.listarMaresCompletas() }
                    DadosServidor(b.await(), n.await(), c.await(), m.await())
                }
            }
            bercos.clear()
            bercos.addAll(dados.bercos.map { b ->
                b.copy(schedules = b.schedules.map { it.copy(berthId = b.id, berthName = b.name) })
            })
            navios.clear(); navios.addAll(dados.navios)
            cargas.clear(); cargas.addAll(dados.cargas)
            mares.clear(); mares.addAll(dados.mares)
            online = true
            ajustarSelecaoDeMes()
            carregarFerrovia()
            carregarRodovia()
            return true
        } catch (e: ApiException) {
            usarDadosLocais(e.message)
        } catch (e: IOException) {
            usarDadosLocais(e.message)
        } catch (e: RuntimeException) {
            usarDadosLocais(e.message)
        }
        return false
    }

    private class DadosFerrovia(
        val estado: PeraHourlyState,
        val ramais: List<RailTerminalBranch>,
        val eixos: List<RailAxisYard>,
        val locomotivas: List<Locomotive>,
        val vagoes: List<Wagon>,
        val alocacoes: List<WagonAllocation>,
        val chegadas: List<LocomotiveArrival>
    )

    /** Carrega o módulo Ferrovia; falhas aqui não derrubam o módulo Portos (só geram aviso). */
    private suspend fun carregarFerrovia() {
        try {
            val d = withContext(Dispatchers.IO) {
                coroutineScope {
                    val e = async { BackendApiClient.obterEstadoPera(peraData, peraHora) }
                    val r = async { BackendApiClient.listarRamais() }
                    val x = async { BackendApiClient.listarEixosPatios() }
                    val l = async { BackendApiClient.listarLocomotivas() }
                    val v = async { BackendApiClient.listarVagoes() }
                    val a = async { BackendApiClient.listarAlocacoes() }
                    val c = async { BackendApiClient.listarChegadas() }
                    DadosFerrovia(e.await(), r.await(), x.await(), l.await(), v.await(), a.await(), c.await())
                }
            }
            aplicarEstadoPera(d.estado)
            railBranches.clear(); railBranches.addAll(d.ramais)
            railAxesYards.clear(); railAxesYards.addAll(d.eixos)
            locomotives.clear(); locomotives.addAll(d.locomotivas)
            wagons.clear(); wagons.addAll(d.vagoes)
            allocations.clear(); allocations.addAll(d.alocacoes)
            railArrivals.clear(); railArrivals.addAll(d.chegadas)
        } catch (e: ApiException) {
            mensagem = "Ferrovia: ${e.message}"
        } catch (e: IOException) {
            mensagem = "Ferrovia: falha de rede (${e.message ?: "sem resposta"})."
        } catch (e: RuntimeException) {
            mensagem = "Ferrovia: erro inesperado (${e.message})."
        }
    }

    private fun aplicarEstadoPera(estado: PeraHourlyState) {
        peraEstado = estado
        dailyTrains.clear(); dailyTrains.addAll(estado.dailySchedule)
    }

    private class DadosServidor(
        val bercos: List<Berth>,
        val navios: List<RegisteredVessel>,
        val cargas: List<PortCargo>,
        val mares: List<TideTable>
    )

    private suspend fun usarDadosLocais(@Suppress("UNUSED_PARAMETER") motivo: String?) {
        val estavaOnline = online
        online = false
        if (bercos.isEmpty() || estavaOnline) {
            val tabuas = withContext(Dispatchers.IO) { TideCsvParser.carregarDosAssets(getApplication()) }
            bercos.clear(); bercos.addAll(SantosPortDefaultData.bercos())
            navios.clear(); navios.addAll(SantosPortDefaultData.navios())
            cargas.clear(); cargas.addAll(SantosPortDefaultData.cargas())
            mares.clear(); mares.addAll(tabuas)
            ajustarSelecaoDeMes()
        }
    }

    private fun ajustarSelecaoDeMes() {
        if (mares.none { it.id == mesSelecionadoId }) {
            mesSelecionadoId = (mareAtiva ?: mares.firstOrNull())?.id
        }
    }

    // ------------------------------------------------------------------ configurações (RF002)

    fun salvarETestarConexao(urlDigitada: String) {
        val url = BackendConfig.normalizar(urlDigitada)
        if (url.isEmpty()) {
            resultadoConexao = ResultadoConexao(false, "Informe a URL do backend.")
            return
        }
        urlBackend = url
        BackendConfig.salvarUrl(getApplication(), url)
        BackendApiClient.baseUrl = url
        viewModelScope.launch {
            carregando = true
            resultadoConexao = null
            try {
                withContext(Dispatchers.IO) { BackendApiClient.checkConnection() }
                resultadoConexao = ResultadoConexao(true, "Conexão estabelecida com $url.")
                online = true
            } catch (e: ApiException) {
                resultadoConexao = ResultadoConexao(false, "O servidor respondeu com erro ${e.codigo}: ${e.message}")
                online = false
            } catch (e: IOException) {
                resultadoConexao = ResultadoConexao(false, "Não foi possível conectar a $url (${e.message ?: "sem resposta"}).")
                online = false
            } catch (e: RuntimeException) {
                resultadoConexao = ResultadoConexao(false, "Erro inesperado: ${e.message}")
                online = false
            }
            carregarDoServidor()
            carregando = false
        }
    }

    fun limparResultadoConexao() {
        resultadoConexao = null
    }

    // ------------------------------------------------------------------ operação genérica

    /**
     * Executa [remoto] quando online (e recarrega tudo depois); quando offline, ou se a rede falhar,
     * aplica [local] apenas na memória do aparelho. Erros HTTP viram Snackbar e os dados são recarregados.
     */
    private fun operar(descricao: String, remoto: suspend () -> Unit, local: () -> Unit) {
        viewModelScope.launch {
            if (!online) {
                local()
                mensagem = "Modo offline: $descricao salvo apenas neste aparelho."
                return@launch
            }
            carregando = true
            try {
                withContext(Dispatchers.IO) { remoto() }
                mensagem = "$descricao concluído."
                carregarDoServidor()
            } catch (e: ApiException) {
                mensagem = e.message ?: "Erro ${e.codigo} do servidor."
                carregarDoServidor()
            } catch (e: IOException) {
                online = false
                local()
                mensagem = "Sem conexão com o backend: $descricao salvo apenas neste aparelho."
            } catch (e: RuntimeException) {
                mensagem = "Erro inesperado: ${e.message}"
            } finally {
                carregando = false
            }
        }
    }

    private fun proximoIdLocal(): Long =
        (minOf(0L, todasAtracacoes.minOfOrNull { it.id ?: 0L } ?: 0L)) - 1

    private fun substituirBerco(id: String, transformar: (Berth) -> Berth) {
        val i = bercos.indexOfFirst { it.id == id }
        if (i >= 0) bercos[i] = transformar(bercos[i])
    }

    // ------------------------------------------------------------------ berços (RF005)

    fun salvarBerco(req: BerthRequest, novo: Boolean) {
        if (novo && bercos.any { it.id.equals(req.id.trim(), ignoreCase = true) }) {
            mensagem = "Já existe um berço com o código ${req.id}."
            return
        }
        operar(
            if (novo) "Cadastro do berço ${req.id}" else "Alteração do berço ${req.id}",
            remoto = {
                if (novo) BackendApiClient.criarBerco(req) else BackendApiClient.atualizarBerco(req.id, req)
            },
            local = {
                if (novo) {
                    bercos.add(
                        Berth(
                            req.id.trim(), req.name.trim(),
                            req.sectorName?.ifBlank { null } ?: "Santos APS",
                            req.maxDraft?.ifBlank { null } ?: "14.0m",
                            req.category, (bercos.maxOfOrNull { it.displayOrder } ?: 0) + 1
                        )
                    )
                } else {
                    substituirBerco(req.id) {
                        it.copy(
                            name = req.name.trim(),
                            sectorName = req.sectorName?.ifBlank { null } ?: it.sectorName,
                            maxDraft = req.maxDraft?.ifBlank { null } ?: it.maxDraft,
                            category = req.category
                        )
                    }
                }
            }
        )
    }

    fun excluirBerco(id: String) {
        operar("Exclusão do berço $id", { BackendApiClient.excluirBerco(id) }, { bercos.removeAll { it.id == id } })
    }

    // ------------------------------------------------------------------ cargas (RF006)

    fun salvarCarga(carga: PortCargo, nova: Boolean) {
        if (nova && cargas.any { it.productCode.equals(carga.productCode, ignoreCase = true) }) {
            mensagem = "Já existe uma carga com o código ${carga.productCode}."
            return
        }
        operar(
            if (nova) "Cadastro da carga ${carga.productCode}" else "Alteração da carga ${carga.productCode}",
            remoto = {
                if (nova) BackendApiClient.criarCarga(carga) else BackendApiClient.atualizarCarga(carga.productCode, carga)
            },
            local = {
                val i = cargas.indexOfFirst { it.productCode == carga.productCode }
                if (i >= 0) cargas[i] = carga else cargas.add(carga)
            }
        )
    }

    fun excluirCarga(codigo: String) {
        operar("Exclusão da carga $codigo", { BackendApiClient.excluirCarga(codigo) }, { cargas.removeAll { it.productCode == codigo } })
    }

    // ------------------------------------------------------------------ navios (RF007)

    fun salvarNavio(navio: RegisteredVessel, novo: Boolean) {
        if (novo && navio.imoCode.isNotBlank() && navios.any { it.imoCode.equals(navio.imoCode, ignoreCase = true) }) {
            mensagem = "Já existe um navio com o IMO ${navio.imoCode}."
            return
        }
        operar(
            if (novo) "Cadastro do navio ${navio.shipName}" else "Alteração do navio ${navio.shipName}",
            remoto = {
                if (novo) BackendApiClient.criarNavio(navio) else BackendApiClient.atualizarNavio(navio.imoCode, navio)
            },
            local = {
                val imo = navio.imoCode.ifBlank { "IMO-LOCAL-${navios.size + 1}" }
                val salvo = navio.copy(imoCode = imo)
                val i = navios.indexOfFirst { it.imoCode == imo }
                if (i >= 0) navios[i] = salvo else navios.add(salvo)
            }
        )
    }

    fun excluirNavio(imo: String) {
        operar("Exclusão do navio $imo", { BackendApiClient.excluirNavio(imo) }, { navios.removeAll { it.imoCode == imo } })
    }

    // ------------------------------------------------------------------ marés (RF003 / RF008)

    fun ativarMare(id: Long) {
        if (mares.firstOrNull { it.id == id }?.activeMonth == true) return
        operar(
            "Ativação da tábua de marés",
            remoto = { BackendApiClient.ativarMare(id) },
            local = {
                val atualizadas = mares.map { it.copy(activeMonth = it.id == id) }
                mares.clear(); mares.addAll(atualizadas)
            }
        )
    }

    // ------------------------------------------------------------------ atracações (RF003/RF004/RF009)

    private fun requisicaoParaAtracacao(id: Long?, req: ShipScheduleRequest): ShipSchedule {
        val berco = bercos.firstOrNull { it.id == req.berthId }
        return ShipSchedule(
            id = id ?: proximoIdLocal(),
            berthId = req.berthId,
            berthName = berco?.name.orEmpty(),
            shipName = req.shipName.trim(),
            unloadingProduct = req.unloadingProduct?.ifBlank { null } ?: "Nenhum (Lastro)",
            loadingProduct = req.loadingProduct?.ifBlank { null } ?: "Nenhum",
            tonnage = if (req.tonnage > 0) req.tonnage else 50000,
            startDay = req.startDay,
            endDay = req.endDay,
            stripeColorHex = req.stripeColorHex?.ifBlank { null } ?: StatusNavio.corHex(req.status),
            status = req.status?.ifBlank { null } ?: StatusNavio.PROGRAMADO,
            durationDays = req.endDay - req.startDay
        )
    }

    /** Valida localmente (RN001/RN002) e persiste. Retorna false se a validação local reprovou. */
    fun salvarAtracacao(id: Long?, req: ShipScheduleRequest): Boolean {
        if (req.shipName.isBlank()) {
            mensagem = "Informe o nome do navio."
            return false
        }
        val destino = bercos.firstOrNull { it.id == req.berthId }
        if (destino == null) {
            mensagem = "Selecione um berço válido."
            return false
        }
        val erro = LineupRules.validarJanela(destino, req.startDay, req.endDay, id, diasMareBaixa)
        if (erro != null) {
            mensagem = erro
            return false
        }
        val novo = id == null
        operar(
            if (novo) "Atracação de ${req.shipName}" else "Alteração da atracação de ${req.shipName}",
            remoto = {
                if (id == null) BackendApiClient.criarAtracacao(req) else BackendApiClient.atualizarAtracacao(id, req)
            },
            local = {
                val nova = requisicaoParaAtracacao(id, req)
                if (id != null) {
                    bercos.forEachIndexed { i, b ->
                        if (b.schedules.any { it.id == id }) bercos[i] = b.copy(schedules = b.schedules.filter { it.id != id })
                    }
                }
                substituirBerco(req.berthId) { it.copy(schedules = (it.schedules + nova).sortedBy { s -> s.startDay }) }
            }
        )
        return true
    }

    fun excluirAtracacao(id: Long) {
        operar(
            "Exclusão da atracação",
            remoto = { BackendApiClient.excluirAtracacao(id) },
            local = {
                bercos.forEachIndexed { i, b ->
                    if (b.schedules.any { it.id == id }) bercos[i] = b.copy(schedules = b.schedules.filter { it.id != id })
                }
            }
        )
    }

    /** Valida (RN001 + RN002) sem persistir; usado para colorir o arrasto no Canvas. */
    fun janelaValida(atracacao: ShipSchedule, ini: Double, fim: Double): Boolean {
        val berco = bercos.firstOrNull { it.id == atracacao.berthId } ?: return false
        return LineupRules.validarJanela(berco, ini, fim, atracacao.id, diasMareBaixa) == null
    }

    /**
     * RF004: move/redimensiona uma atracação. Valida localmente; se inválido nada muda (o Canvas volta à
     * posição original) e um alerta é exibido. Aplica de forma otimista e reverte se o servidor recusar.
     */
    fun moverAtracacao(atracacao: ShipSchedule, ini: Double, fim: Double) {
        val id = atracacao.id ?: return
        if (ini == atracacao.startDay && fim == atracacao.endDay) return
        val berco = bercos.firstOrNull { it.id == atracacao.berthId }
        if (berco == null) {
            mensagem = "Berço da atracação não encontrado."
            return
        }
        val erro = LineupRules.validarJanela(berco, ini, fim, id, diasMareBaixa)
        if (erro != null) {
            mensagem = "$erro A atracação voltou para a posição original."
            return
        }
        val copia = bercos.toList()
        fun aplicar() = substituirBerco(berco.id) { b ->
            b.copy(schedules = b.schedules.map { if (it.id == id) it.copy(startDay = ini, endDay = fim, durationDays = fim - ini) else it }
                .sortedBy { it.startDay })
        }
        aplicar()
        if (!online) {
            mensagem = "Modo offline: alteração salva apenas neste aparelho."
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    BackendApiClient.moverAtracacao(id, ShipScheduleWindowUpdateRequest(ini, fim, berco.id))
                }
            } catch (e: ApiException) {
                bercos.clear(); bercos.addAll(copia)
                mensagem = (e.message ?: "O servidor recusou a alteração (${e.codigo}).") + " A atracação voltou para a posição original."
            } catch (e: IOException) {
                online = false
                mensagem = "Sem conexão com o backend: alteração salva apenas neste aparelho."
            } catch (e: RuntimeException) {
                bercos.clear(); bercos.addAll(copia)
                mensagem = "Erro inesperado: ${e.message}. A atracação voltou para a posição original."
            }
        }
    }

    // ------------------------------------------------------------------ ferrovia: Pera (RF010)

    fun alterarPeraHora(hora: Int) {
        peraHora = RailRules.horaValida(hora)
        agendarConsultaPera()
    }

    fun alterarPeraData(data: String) {
        peraData = data
        agendarConsultaPera()
    }

    /** Consulta o estado da Pera com um pequeno atraso, para não disparar uma chamada a cada passo do Slider. */
    private fun agendarConsultaPera() {
        jobPera?.cancel()
        if (!online) return
        jobPera = viewModelScope.launch {
            delay(250)
            try {
                val estado = withContext(Dispatchers.IO) { BackendApiClient.obterEstadoPera(peraData, peraHora) }
                aplicarEstadoPera(estado)
            } catch (e: CancellationException) {
                throw e // consulta substituída por outra (Slider/data); não é erro
            } catch (e: ApiException) {
                mensagem = "Pera: ${e.message}"
            } catch (e: IOException) {
                mensagem = "Pera: falha de rede (${e.message ?: "sem resposta"})."
            } catch (e: RuntimeException) {
                mensagem = "Pera: erro inesperado (${e.message})."
            }
        }
    }

    // ------------------------------------------------------------------ ferrovia: ramais/AMV (RF011)

    /** PATCH /api/rail/branches/{code}/amv. Atualiza o ramal com o JSON devolvido (copy). */
    fun toggleRailBranchAmv(code: String, active: Boolean) {
        val i = railBranches.indexOfFirst { it.code == code }
        if (i < 0) return
        if (!online) {
            railBranches[i] = railBranches[i].copy(switchActive = active)
            mensagem = "Modo offline: AMV de $code alterado apenas neste aparelho."
            return
        }
        viewModelScope.launch {
            try {
                val novo = withContext(Dispatchers.IO) { BackendApiClient.alternarAmv(code, active) }
                val j = railBranches.indexOfFirst { it.code == code }
                if (j >= 0) railBranches[j] = railBranches[j].copy(switchActive = novo.switchActive)
                agendarConsultaPera()
            } catch (e: ApiException) {
                mensagem = e.message ?: "Erro ${e.codigo} do servidor."
            } catch (e: IOException) {
                online = false
                mensagem = "Sem conexão com o backend: AMV não alterado."
            } catch (e: RuntimeException) {
                mensagem = "Erro inesperado: ${e.message}"
            }
        }
    }

    // ------------------------------------------------------------------ ferrovia: locomotivas (RF013)

    fun salvarLocomotiva(req: LocomotiveRequest, nova: Boolean) {
        if (nova && locomotives.any { it.locomotiveCode.equals(req.locomotiveCode.trim(), ignoreCase = true) }) {
            mensagem = "Já existe uma locomotiva com o código ${req.locomotiveCode}."
            return
        }
        val limpa = req.copy(
            locomotiveCode = req.locomotiveCode.trim(),
            modelPower = req.modelPower.trim(),
            operatorAxis = req.operatorAxis.trim()
        )
        operar(
            if (nova) "Cadastro da locomotiva ${limpa.locomotiveCode}" else "Alteração da locomotiva ${limpa.locomotiveCode}",
            remoto = {
                if (nova) BackendApiClient.criarLocomotiva(limpa)
                else BackendApiClient.atualizarLocomotiva(limpa.locomotiveCode, limpa)
            },
            local = {
                val i = locomotives.indexOfFirst { it.locomotiveCode == limpa.locomotiveCode }
                if (i >= 0) locomotives[i] = locomotives[i].copy(modelPower = limpa.modelPower, operatorAxis = limpa.operatorAxis)
                else locomotives.add(Locomotive(limpa.locomotiveCode, limpa.modelPower, limpa.operatorAxis))
            }
        )
    }

    fun excluirLocomotiva(codigo: String) {
        operar(
            "Exclusão da locomotiva $codigo",
            { BackendApiClient.excluirLocomotiva(codigo) },
            {
                locomotives.removeAll { it.locomotiveCode == codigo }
                wagons.removeAll { it.locomotiveCode == codigo }
            }
        )
    }

    // ------------------------------------------------------------------ ferrovia: vagões (RF014)

    fun salvarVagao(req: WagonRequest, novo: Boolean) {
        if (novo && wagons.any { it.wagonCode.equals(req.wagonCode.trim(), ignoreCase = true) }) {
            mensagem = "Já existe um vagão com o código ${req.wagonCode}."
            return
        }
        val limpo = req.copy(
            wagonCode = req.wagonCode.trim(), product = req.product.trim(), capacity = req.capacity.trim(),
            originTerminal = req.originTerminal.trim(), destinationTerminal = req.destinationTerminal.trim()
        )
        operar(
            if (novo) "Cadastro do vagão ${limpo.wagonCode}" else "Alteração do vagão ${limpo.wagonCode}",
            remoto = {
                if (novo) BackendApiClient.criarVagao(limpo) else BackendApiClient.atualizarVagao(limpo.wagonCode, limpo)
            },
            local = {
                val v = Wagon(limpo.wagonCode, limpo.product, limpo.capacity, limpo.originTerminal, limpo.destinationTerminal, limpo.locomotiveCode)
                val i = wagons.indexOfFirst { it.wagonCode == v.wagonCode }
                if (i >= 0) wagons[i] = v else wagons.add(v)
            }
        )
    }

    fun excluirVagao(codigo: String) {
        operar("Exclusão do vagão $codigo", { BackendApiClient.excluirVagao(codigo) }, { wagons.removeAll { it.wagonCode == codigo } })
    }

    // ------------------------------------------------------------------ ferrovia: alocação (RF015)

    /** Próximo ID "ALC-nnn" livre (o backend usa count+1, que pode colidir depois de exclusões). */
    private fun proximoIdAlocacao(): String {
        val n = (allocations.maxOfOrNull { it.allocationId.filter(Char::isDigit).toIntOrNull() ?: 0 } ?: 0) + 1
        return "ALC-" + n.toString().padStart(3, '0')
    }

    fun salvarAlocacao(a: WagonAllocation, nova: Boolean) {
        val salva = if (nova && a.allocationId.isBlank()) a.copy(allocationId = proximoIdAlocacao()) else a
        operar(
            if (nova) "Cadastro da alocação ${salva.allocationId}" else "Alteração da alocação ${salva.allocationId}",
            remoto = {
                if (nova) BackendApiClient.criarAlocacao(salva) else BackendApiClient.atualizarAlocacao(salva.allocationId, salva)
            },
            local = {
                val i = allocations.indexOfFirst { it.allocationId == salva.allocationId }
                if (i >= 0) allocations[i] = salva else allocations.add(salva)
            }
        )
    }

    fun excluirAlocacao(id: String) {
        operar("Exclusão da alocação $id", { BackendApiClient.excluirAlocacao(id) }, { allocations.removeAll { it.allocationId == id } })
    }

    // ------------------------------------------------------------------ ferrovia: chegadas (RF016)

    private fun proximoIdChegada(): String {
        val n = (railArrivals.maxOfOrNull { it.arrivalId.filter(Char::isDigit).toIntOrNull() ?: 0 } ?: 100) + 1
        return "ARR-$n"
    }

    fun salvarChegada(c: LocomotiveArrival, nova: Boolean) {
        val salva = if (nova && c.arrivalId.isBlank()) c.copy(arrivalId = proximoIdChegada()) else c
        operar(
            if (nova) "Cadastro da chegada ${salva.arrivalId}" else "Alteração da chegada ${salva.arrivalId}",
            remoto = {
                if (nova) BackendApiClient.criarChegada(salva) else BackendApiClient.atualizarChegada(salva.arrivalId, salva)
            },
            local = {
                val i = railArrivals.indexOfFirst { it.arrivalId == salva.arrivalId }
                if (i >= 0) railArrivals[i] = salva else railArrivals.add(salva)
            }
        )
    }

    fun excluirChegada(id: String) {
        operar("Exclusão da chegada $id", { BackendApiClient.excluirChegada(id) }, { railArrivals.removeAll { it.arrivalId == id } })
    }

    // ------------------------------------------------------------------ rodovia: carga (RF017 a RF022)

    private class DadosRodovia(
        val docas: List<HighwayLine>,
        val setores: List<HighwaySector>,
        val caminhoes: List<RegisteredTruck>,
        val cargas: List<HighwayCargo>
    )

    /** Carrega o módulo Rodovia; falhas aqui não derrubam Portos nem Ferrovia (só geram aviso). */
    private suspend fun carregarRodovia() {
        try {
            val d = withContext(Dispatchers.IO) {
                coroutineScope {
                    val l = async { BackendApiClient.listarDocas() }
                    val s = async { BackendApiClient.listarSetores() }
                    val t = async { BackendApiClient.listarCaminhoes() }
                    val c = async { BackendApiClient.listarCargasRodoviarias() }
                    DadosRodovia(l.await(), s.await(), t.await(), c.await())
                }
            }
            highwayLines.clear(); highwayLines.addAll(d.docas.sortedBy { it.lineNumber })
            highwaySectors.clear(); highwaySectors.addAll(d.setores)
            registeredTrucks.clear(); registeredTrucks.addAll(d.caminhoes)
            highwayCargos.clear(); highwayCargos.addAll(d.cargas)
        } catch (e: ApiException) {
            mensagem = "Rodovia: ${e.message}"
        } catch (e: IOException) {
            mensagem = "Rodovia: falha de rede (${e.message ?: "sem resposta"})."
        } catch (e: RuntimeException) {
            mensagem = "Rodovia: erro inesperado (${e.message})."
        }
    }

    /**
     * Executa [remoto] e depois [recarregar] (só a lista afetada). A Rodovia não tem dados offline:
     * sem conexão a alteração é recusada com aviso. Erros HTTP e de rede viram Snackbar.
     */
    private fun operarRodovia(descricao: String, remoto: suspend () -> Unit, recarregar: suspend () -> Unit) {
        if (!online) {
            mensagem = "Sem conexão com o backend: $descricao exige a API online."
            return
        }
        viewModelScope.launch {
            carregando = true
            try {
                withContext(Dispatchers.IO) { remoto() }
                mensagem = "$descricao concluído."
                recarregar()
            } catch (e: ApiException) {
                mensagem = e.message ?: "Erro ${e.codigo} do servidor."
                recarregar()
            } catch (e: IOException) {
                online = false
                mensagem = "Sem conexão com o backend: $descricao não foi salvo."
            } catch (e: RuntimeException) {
                mensagem = "Erro inesperado: ${e.message}"
            } finally {
                carregando = false
            }
        }
    }

    /** Recarrega uma lista da Rodovia; falhas viram aviso e mantêm a lista atual. */
    private suspend fun <T> recarregarLista(destino: MutableList<T>, buscar: suspend () -> List<T>) {
        try {
            val novos = withContext(Dispatchers.IO) { buscar() }
            destino.clear(); destino.addAll(novos)
        } catch (e: ApiException) {
            mensagem = "Rodovia: ${e.message}"
        } catch (e: IOException) {
            mensagem = "Rodovia: falha de rede (${e.message ?: "sem resposta"})."
        } catch (e: RuntimeException) {
            mensagem = "Rodovia: erro inesperado (${e.message})."
        }
    }

    // ------------------------------------------------------------------ rodovia: agendamentos (RF017 / RF022)

    private fun substituirDoca(numero: Int, transformar: (HighwayLine) -> HighwayLine) {
        val i = highwayLines.indexOfFirst { it.lineNumber == numero }
        if (i >= 0) highwayLines[i] = transformar(highwayLines[i])
    }

    private fun comTrucks(doca: HighwayLine, trucks: List<TruckSchedule>): HighwayLine {
        val ordenados = trucks.sortedBy { it.startSlot }
        return doca.copy(trucks = ordenados, truckCount = ordenados.size)
    }

    private fun restaurarDocas(antes: List<HighwayLine>) {
        antes.forEach { original -> substituirDoca(original.lineNumber) { original } }
    }

    private fun requisicaoDeAgendamento(lineNumber: Int, t: TruckSchedule, novo: Boolean) = TruckScheduleRequest(
        // O truckId de um novo agendamento NÃO é gerado aqui: vai nulo e o backend cria (CAM-lll-nnnnn).
        truckId = if (novo) null else t.truckId,
        lineNumber = lineNumber,
        plateAndFleet = t.plateAndFleet.trim(),
        carrierName = t.carrierName.trim(),
        unloadingProduct = t.unloadingProduct.trim(),
        loadingProduct = t.loadingProduct.trim(),
        tonnage = t.tonnage,
        startSlot = t.startSlot,
        endSlot = t.endSlot,
        colorHex = t.colorHex.ifBlank { null }
    )

    /**
     * Cria (truckId vazio) ou altera um agendamento na doca [lineNumber], movendo-o de doca se preciso.
     * Valida RN016/RN017 localmente; aplica de forma otimista e reverte se o servidor recusar (400/404/409/500).
     * Devolve false se a validação local reprovou (a mensagem vai para o Snackbar).
     */
    fun addOrUpdateTruck(lineNumber: Int, truck: TruckSchedule): Boolean {
        val destino = highwayLines.firstOrNull { it.lineNumber == lineNumber }
        if (destino == null) {
            mensagem = "Selecione uma doca válida (1 a ${HighwayRules.DOCAS})."
            return false
        }
        if (truck.truckId.startsWith(HighwayRules.PREFIXO_ID_TEMPORARIO)) {
            mensagem = "Este agendamento ainda está sendo salvo. Aguarde um instante."
            return false
        }
        val novo = truck.truckId.isBlank()
        val erro = HighwayRules.validarJanela(destino, truck.startSlot, truck.endSlot, truck.truckId.ifBlank { null })
        if (erro != null) {
            mensagem = erro
            return false
        }
        if (truck.plateAndFleet.isBlank() || truck.carrierName.isBlank() ||
            truck.unloadingProduct.isBlank() || truck.loadingProduct.isBlank()
        ) {
            mensagem = "Preencha placa/frota, transportadora e os produtos de descarga e retorno."
            return false
        }
        if (!online) {
            mensagem = "Sem conexão com o backend: o agendamento exige a API online."
            return false
        }
        val origem = if (novo) null else highwayLines.firstOrNull { l -> l.trucks.any { it.truckId == truck.truckId } }
        val idLocal = if (novo) "${HighwayRules.PREFIXO_ID_TEMPORARIO}${++contadorIdTemporario}" else truck.truckId
        val antes = listOfNotNull(destino, origem).distinctBy { it.lineNumber }
        val otimista = truck.copy(
            truckId = idLocal, lineNumber = lineNumber, lineCode = destino.lineCode,
            colorHex = truck.colorHex.ifBlank { destino.sectorColorHex },
            durationSlots = truck.endSlot - truck.startSlot,
            windowFormatted = "${HighwayRules.formatarSlot(truck.startSlot)} → ${HighwayRules.formatarSlot(truck.endSlot)}"
        )
        if (origem != null && origem.lineNumber != lineNumber) {
            substituirDoca(origem.lineNumber) { comTrucks(it, it.trucks.filter { t -> t.truckId != truck.truckId }) }
        }
        substituirDoca(lineNumber) { comTrucks(it, it.trucks.filter { t -> t.truckId != truck.truckId } + otimista) }

        viewModelScope.launch {
            try {
                val req = requisicaoDeAgendamento(lineNumber, truck, novo)
                val salvo = withContext(Dispatchers.IO) {
                    if (novo) BackendApiClient.criarAgendamento(req) else BackendApiClient.atualizarAgendamento(truck.truckId, req)
                }
                substituirDoca(lineNumber) { d -> comTrucks(d, d.trucks.filter { it.truckId != idLocal } + salvo) }
                mensagem = if (novo) "Agendamento de ${salvo.plateAndFleet} criado." else "Agendamento de ${salvo.plateAndFleet} alterado."
            } catch (e: ApiException) {
                restaurarDocas(antes)
                mensagem = (e.message ?: "O servidor recusou o agendamento (${e.codigo}).") + " A alteração foi desfeita."
            } catch (e: IOException) {
                restaurarDocas(antes)
                online = false
                mensagem = "Sem conexão com o backend: o agendamento não foi salvo e a alteração foi desfeita."
            } catch (e: RuntimeException) {
                restaurarDocas(antes)
                mensagem = "Erro inesperado: ${e.message}. A alteração foi desfeita."
            }
        }
        return true
    }

    /** Exclui o agendamento [truckId] (otimista; reverte se o servidor recusar). */
    fun deleteTruck(truckId: String) {
        if (truckId.startsWith(HighwayRules.PREFIXO_ID_TEMPORARIO)) {
            mensagem = "Este agendamento ainda está sendo salvo. Aguarde um instante."
            return
        }
        val doca = highwayLines.firstOrNull { l -> l.trucks.any { it.truckId == truckId } } ?: return
        if (!online) {
            mensagem = "Sem conexão com o backend: a exclusão exige a API online."
            return
        }
        val antes = listOf(doca)
        substituirDoca(doca.lineNumber) { comTrucks(it, it.trucks.filter { t -> t.truckId != truckId }) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { BackendApiClient.excluirAgendamento(truckId) }
                mensagem = "Agendamento excluído."
            } catch (e: ApiException) {
                restaurarDocas(antes)
                mensagem = (e.message ?: "O servidor recusou a exclusão (${e.codigo}).") + " O agendamento foi restaurado."
            } catch (e: IOException) {
                restaurarDocas(antes)
                online = false
                mensagem = "Sem conexão com o backend: a exclusão foi desfeita."
            } catch (e: RuntimeException) {
                restaurarDocas(antes)
                mensagem = "Erro inesperado: ${e.message}. A exclusão foi desfeita."
            }
        }
    }

    // ------------------------------------------------------------------ rodovia: caminhões cadastrados (RF020)

    fun addOrUpdateRegisteredTruck(caminhao: RegisteredTruck, novo: Boolean) {
        val limpo = caminhao.copy(
            truckCode = caminhao.truckCode.trim(),
            plateAndFleet = caminhao.plateAndFleet.trim(),
            carrierName = caminhao.carrierName.trim(),
            truckConfigType = caminhao.truckConfigType.trim(),
            defaultUnloadProduct = caminhao.defaultUnloadProduct.trim(),
            defaultLoadProduct = caminhao.defaultLoadProduct.trim()
        )
        if (limpo.truckCode.isEmpty()) {
            mensagem = "Informe o código do caminhão."
            return
        }
        if (novo && registeredTrucks.any { it.truckCode.equals(limpo.truckCode, ignoreCase = true) }) {
            mensagem = "Já existe um caminhão com o código ${limpo.truckCode}."
            return
        }
        operarRodovia(
            if (novo) "Cadastro do caminhão ${limpo.truckCode}" else "Alteração do caminhão ${limpo.truckCode}",
            remoto = {
                if (novo) BackendApiClient.criarCaminhao(limpo) else BackendApiClient.atualizarCaminhao(limpo.truckCode, limpo)
            },
            recarregar = { recarregarLista(registeredTrucks) { BackendApiClient.listarCaminhoes() } }
        )
    }

    fun deleteRegisteredTruck(codigo: String) {
        operarRodovia(
            "Exclusão do caminhão $codigo",
            remoto = { BackendApiClient.excluirCaminhao(codigo) },
            recarregar = { recarregarLista(registeredTrucks) { BackendApiClient.listarCaminhoes() } }
        )
    }

    // ------------------------------------------------------------------ rodovia: cargas (RF021)

    fun addOrUpdateHighwayCargo(carga: HighwayCargo, nova: Boolean) {
        val limpa = carga.copy(
            cargoCode = carga.cargoCode.trim(), productName = carga.productName.trim(),
            equipmentType = carga.equipmentType.trim(), defaultStayDuration = carga.defaultStayDuration.trim()
        )
        if (limpa.cargoCode.isEmpty()) {
            mensagem = "Informe o código da carga."
            return
        }
        if (nova && highwayCargos.any { it.cargoCode.equals(limpa.cargoCode, ignoreCase = true) }) {
            mensagem = "Já existe uma carga rodoviária com o código ${limpa.cargoCode}."
            return
        }
        operarRodovia(
            if (nova) "Cadastro da carga ${limpa.cargoCode}" else "Alteração da carga ${limpa.cargoCode}",
            remoto = {
                if (nova) BackendApiClient.criarCargaRodoviaria(limpa) else BackendApiClient.atualizarCargaRodoviaria(limpa.cargoCode, limpa)
            },
            recarregar = { recarregarLista(highwayCargos) { BackendApiClient.listarCargasRodoviarias() } }
        )
    }

    fun deleteHighwayCargo(codigo: String) {
        operarRodovia(
            "Exclusão da carga $codigo",
            remoto = { BackendApiClient.excluirCargaRodoviaria(codigo) },
            recarregar = { recarregarLista(highwayCargos) { BackendApiClient.listarCargasRodoviarias() } }
        )
    }
}
