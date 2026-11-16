package br.edu.unirv.lineup.ui.navigation

enum class GrupoMenu(val titulo: String) {
    PORTOS("PORTOS"),
    FERROVIA("FERROVIA"),
    RODOVIA("RODOVIA"),
    SISTEMA("SISTEMA")
}

/** Todas as telas do app (19 de negócio + Configurações). */
sealed class Screen(val rota: String, val titulo: String, val grupo: GrupoMenu) {

    // Portos
    data object Lineup : Screen("lineup", "Lineup", GrupoMenu.PORTOS)
    data object Bercos : Screen("bercos", "Berços", GrupoMenu.PORTOS)
    data object Cargas : Screen("cargas", "Cargas", GrupoMenu.PORTOS)
    data object Navios : Screen("navios", "Navios", GrupoMenu.PORTOS)
    data object Mares : Screen("mares", "Marés", GrupoMenu.PORTOS)
    data object Atracacoes : Screen("atracacoes", "Atracações", GrupoMenu.PORTOS)

    // Ferrovia
    data object Pera : Screen("pera", "Pera", GrupoMenu.FERROVIA)
    data object MoegasAmvs : Screen("moegas_amvs", "Moegas/AMVs", GrupoMenu.FERROVIA)
    data object EixosPatios : Screen("eixos_patios", "Eixos & Pátios", GrupoMenu.FERROVIA)
    data object Locomotivas : Screen("locomotivas", "Locomotivas", GrupoMenu.FERROVIA)
    data object Vagoes : Screen("vagoes", "Vagões", GrupoMenu.FERROVIA)
    data object Alocacao : Screen("alocacao", "Alocação", GrupoMenu.FERROVIA)
    data object Chegadas : Screen("chegadas", "Chegadas", GrupoMenu.FERROVIA)

    // Rodovia
    data object Gantt : Screen("gantt", "Gantt", GrupoMenu.RODOVIA)
    data object Docas : Screen("docas", "Docas", GrupoMenu.RODOVIA)
    data object Setores : Screen("setores", "Setores", GrupoMenu.RODOVIA)
    data object Caminhoes : Screen("caminhoes", "Caminhões", GrupoMenu.RODOVIA)
    data object CargasRodoviarias : Screen("cargas_rodoviarias", "Cargas Rodoviárias", GrupoMenu.RODOVIA)
    data object Agendamentos : Screen("agendamentos", "Agendamentos", GrupoMenu.RODOVIA)

    // Sistema
    data object Settings : Screen("settings", "Configurações", GrupoMenu.SISTEMA)

    companion object {
        val todas: List<Screen> by lazy {
            listOf(
                Lineup, Bercos, Cargas, Navios, Mares, Atracacoes,
                Pera, MoegasAmvs, EixosPatios, Locomotivas, Vagoes, Alocacao, Chegadas,
                Gantt, Docas, Setores, Caminhoes, CargasRodoviarias, Agendamentos,
                Settings
            )
        }

        fun doGrupo(grupo: GrupoMenu): List<Screen> = todas.filter { it.grupo == grupo }
    }
}
