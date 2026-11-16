package br.edu.unirv.lineup.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.edu.unirv.lineup.MainViewModel
import br.edu.unirv.lineup.ui.screens.highway.HighwayAppointmentsScreen
import br.edu.unirv.lineup.ui.screens.highway.HighwayCargosScreen
import br.edu.unirv.lineup.ui.screens.highway.HighwayDocksScreen
import br.edu.unirv.lineup.ui.screens.highway.HighwayGanttScreen
import br.edu.unirv.lineup.ui.screens.highway.HighwaySectorsScreen
import br.edu.unirv.lineup.ui.screens.highway.HighwayTrucksScreen
import br.edu.unirv.lineup.ui.screens.port.AtracacoesScreen
import br.edu.unirv.lineup.ui.screens.port.BercosScreen
import br.edu.unirv.lineup.ui.screens.port.CargasScreen
import br.edu.unirv.lineup.ui.screens.port.LineupScreen
import br.edu.unirv.lineup.ui.screens.port.MaresScreen
import br.edu.unirv.lineup.ui.screens.port.NaviosScreen
import br.edu.unirv.lineup.ui.screens.rail.RailAllocationScreen
import br.edu.unirv.lineup.ui.screens.rail.RailArrivalsScreen
import br.edu.unirv.lineup.ui.screens.rail.RailAxesScreen
import br.edu.unirv.lineup.ui.screens.rail.RailBranchesScreen
import br.edu.unirv.lineup.ui.screens.rail.RailLocomotivesScreen
import br.edu.unirv.lineup.ui.screens.rail.RailPeraScreen
import br.edu.unirv.lineup.ui.screens.rail.RailWagonsScreen
import br.edu.unirv.lineup.ui.screens.settings.SettingsScreen
import br.edu.unirv.lineup.ui.theme.Paleta
import kotlinx.coroutines.launch

/**
 * Raiz do app: gaveta de navegação (RF001), barra superior e roteamento por estado.
 * É o único composable que conhece o MainViewModel; as telas recebem apenas estado e lambdas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel) {
    val gaveta = rememberDrawerState(DrawerValue.Closed)
    val escopo = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val tela = vm.telaAtual

    LaunchedEffect(vm.mensagem) {
        vm.mensagem?.let {
            snackbar.showSnackbar(it)
            vm.consumirMensagem()
        }
    }

    BackHandler(enabled = gaveta.isOpen || tela != Screen.Lineup) {
        if (gaveta.isOpen) escopo.launch { gaveta.close() } else vm.navegarPara(Screen.Lineup)
    }

    fun ir(destino: Screen) {
        vm.navegarPara(destino)
        escopo.launch { gaveta.close() }
    }

    ModalNavigationDrawer(
        drawerState = gaveta,
        drawerContent = {
            ModalDrawerSheet {
                ConteudoDaGaveta(
                    telaAtual = tela,
                    online = vm.online,
                    aoIr = ::ir
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(tela.titulo, style = MaterialTheme.typography.titleLarge)
                            Text(
                                tela.grupo.titulo,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { escopo.launch { gaveta.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menu")
                        }
                    },
                    actions = {
                        if (vm.carregando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(onClick = { vm.loadAllData() }, enabled = !vm.carregando) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Paleta.Marinho,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            val corpo = Modifier.fillMaxSize().padding(padding)
            when (tela) {
                Screen.Lineup -> LineupScreen(
                    kpis = vm.kpis,
                    bercosDaPagina = vm.bercosDaPagina,
                    todosBercos = vm.bercos,
                    navios = vm.navios,
                    cargas = vm.cargas,
                    mares = vm.mares,
                    mareAtiva = vm.mareAtiva,
                    diasMareBaixa = vm.diasMareBaixa,
                    busca = vm.busca,
                    categoriasSelecionadas = vm.categoriasSelecionadas,
                    pagina = vm.pagina,
                    totalPaginas = vm.totalPaginas,
                    janelaValida = vm::janelaValida,
                    aoBuscar = vm::alterarBusca,
                    aoAlternarCategoria = vm::alternarCategoria,
                    aoLimparCategorias = vm::limparCategorias,
                    aoAtivarMare = vm::ativarMare,
                    aoMudarPagina = vm::irParaPagina,
                    aoSalvarAtracacao = vm::salvarAtracacao,
                    aoExcluirAtracacao = vm::excluirAtracacao,
                    aoMoverAtracacao = vm::moverAtracacao,
                    aoSalvarBerco = vm::salvarBerco,
                    modifier = corpo
                )
                Screen.Bercos -> BercosScreen(vm.bercos, vm::salvarBerco, vm::excluirBerco, corpo)
                Screen.Cargas -> CargasScreen(vm.cargas, vm::salvarCarga, vm::excluirCarga, corpo)
                Screen.Navios -> NaviosScreen(vm.navios, vm::salvarNavio, vm::excluirNavio, corpo)
                Screen.Mares -> MaresScreen(vm.mares, vm.mesSelecionadoId, vm::selecionarMes, vm::ativarMare, corpo)
                Screen.Atracacoes -> AtracacoesScreen(
                    atracacoes = vm.todasAtracacoes,
                    bercos = vm.bercos,
                    navios = vm.navios,
                    cargas = vm.cargas,
                    diasMareBaixa = vm.diasMareBaixa,
                    aoSalvar = vm::salvarAtracacao,
                    aoExcluir = vm::excluirAtracacao,
                    modifier = corpo
                )
                Screen.Settings -> SettingsScreen(
                    urlAtual = vm.urlBackend,
                    resultado = vm.resultadoConexao,
                    testando = vm.carregando,
                    aoSalvarETestar = vm::salvarETestarConexao,
                    modifier = corpo
                )
                Screen.Pera -> RailPeraScreen(
                    estado = vm.peraEstado,
                    data = vm.peraData,
                    hora = vm.peraHora,
                    trens = vm.dailyTrains,
                    ramais = vm.railBranches,
                    eixos = vm.railAxesYards,
                    aoMudarHora = vm::alterarPeraHora,
                    aoMudarData = vm::alterarPeraData,
                    modifier = corpo
                )
                Screen.MoegasAmvs -> RailBranchesScreen(vm.railBranches, vm::toggleRailBranchAmv, corpo)
                Screen.EixosPatios -> RailAxesScreen(vm.railAxesYards, corpo)
                Screen.Locomotivas -> RailLocomotivesScreen(
                    vm.locomotives, vm.wagons, vm::salvarLocomotiva, vm::excluirLocomotiva,
                    vm::salvarVagao, vm::excluirVagao, corpo
                )
                Screen.Vagoes -> RailWagonsScreen(vm.locomotives, vm.wagons, vm::salvarVagao, vm::excluirVagao, corpo)
                Screen.Alocacao -> RailAllocationScreen(
                    vm.allocations, vm.locomotives, vm.wagons, vm::salvarAlocacao, vm::excluirAlocacao, corpo
                )
                Screen.Chegadas -> RailArrivalsScreen(
                    vm.railArrivals, vm.locomotives, vm::salvarChegada, vm::excluirChegada, corpo
                )
                Screen.Gantt -> HighwayGanttScreen(
                    vm.highwayLines, vm.registeredTrucks, vm::addOrUpdateTruck, vm::deleteTruck, corpo
                )
                Screen.Docas -> HighwayDocksScreen(vm.highwayLines, corpo)
                Screen.Setores -> HighwaySectorsScreen(vm.highwaySectors, vm.highwayLines, corpo)
                Screen.Caminhoes -> HighwayTrucksScreen(
                    vm.registeredTrucks, vm::addOrUpdateRegisteredTruck, vm::deleteRegisteredTruck, corpo
                )
                Screen.CargasRodoviarias -> HighwayCargosScreen(
                    vm.highwayCargos, vm::addOrUpdateHighwayCargo, vm::deleteHighwayCargo, corpo
                )
                Screen.Agendamentos -> HighwayAppointmentsScreen(
                    vm.highwayLines, vm.registeredTrucks, vm::addOrUpdateTruck, vm::deleteTruck, corpo
                )
            }
        }
    }
}

@Composable
private fun ConteudoDaGaveta(telaAtual: Screen, online: Boolean, aoIr: (Screen) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().background(Paleta.Marinho).padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Text("Lineup Santos", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                "Gêmeo digital do Porto de Santos",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f)
            )
            Spacer(Modifier.height(14.dp))
            PilulaDeStatus(online)
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            listOf(GrupoMenu.PORTOS, GrupoMenu.FERROVIA, GrupoMenu.RODOVIA).forEach { grupo ->
                Text(
                    grupo.titulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
                Screen.doGrupo(grupo).forEach { destino ->
                    NavigationDrawerItem(
                        label = { Text(destino.titulo) },
                        selected = destino == telaAtual,
                        onClick = { aoIr(destino) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }

        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { aoIr(Screen.Vagoes) }, modifier = Modifier.weight(1f)) {
                Text("Modal Vagões", maxLines = 1)
            }
            OutlinedButton(onClick = { aoIr(Screen.Settings) }, modifier = Modifier.weight(1f)) {
                Text("Configurações", maxLines = 1)
            }
        }
    }
}

@Composable
private fun PilulaDeStatus(online: Boolean) {
    val cor = if (online) Paleta.Online else Paleta.Offline
    Surface(shape = RoundedCornerShape(50), color = cor) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(8.dp)) {}
            Spacer(Modifier.width(8.dp))
            Text(
                if (online) "API Online (LineupBackend)" else "Modo Local / Offline",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}
