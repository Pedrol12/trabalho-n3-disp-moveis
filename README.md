# Lineup Santos

Aplicativo Android (Kotlin, Jetpack Compose, Material 3) do trabalho N3 de Dispositivos Móveis: gêmeo digital do Porto de Santos.
Consome o LineupBackend (Spring Boot, PostgreSQL) do professor.

Estado atual: **fase 1** (fundação + módulo Portos). Ferrovia e Rodovia aparecem no menu como "Em construção" (fases 2 e 3).

## Arquitetura

MVVM com state hoisting. `MainViewModel` (AndroidViewModel) guarda todo o estado em `mutableStateOf`/`mutableStateListOf`
e é o único que chama o `BackendApiClient` (coroutines em `viewModelScope` + `withContext(Dispatchers.IO)`).
As telas `@Composable` só recebem estado e lambdas.

```
br.edu.unirv.lineup
  MainActivity, MainViewModel
  data/model    modelos (nomes de campos idênticos ao JSON do backend), CargoCategory, regras do Lineup
  data/remote   BackendConfig (URL em SharedPreferences), BackendApiClient (OkHttp + Gson)
  data/local    SantosPortDefaultData (contingência offline), TideCsvParser (assets/tides/*.csv)
  ui/navigation Screen (sealed class), AppRoot (ModalNavigationDrawer + TopAppBar)
  ui/components diálogos, linha do tempo (Canvas), cartões
  ui/screens    port/, rail/, highway/, settings/
  ui/theme
```

Regras validadas localmente antes de persistir: RN001 (colisão no berço, `Berth.hasOverlap`), RN002 (dias de maré baixa
da tábua ativa) e RN004 (duração sugerida = ceil(toneladas / taxa diária)). Se o servidor devolver 400/404/409/500 a mensagem aparece
em Snackbar e a alteração é revertida. Sem rede, o app muda para "Modo Local / Offline" e usa `SantosPortDefaultData`
e as tábuas de maré dos assets; edições feitas offline ficam só na memória do aparelho.

## Como subir o backend

1. Suba o PostgreSQL local com o banco `lineup_santos` (usuário `postgres`, senha `unirv`, conforme `application.properties`).
2. Em `C:\IdeaProjects\LineupBackend`, rode a classe `org.example.Main` pelo IntelliJ (ou `mvn spring-boot:run`).
   O servidor sobe em `http://localhost:8080`; teste em `http://localhost:8080/api/port/tides/active`.
3. Na primeira execução o backend popula berços, navios, cargas e as tábuas de maré.

## Como apontar o app para o backend

Abra o menu, **Configurações**, escolha um preset ou digite a URL e toque em **Salvar e Testar Conexão**.

| Cenário | URL | Observação |
|---|---|---|
| Emulador Android | `http://10.0.2.2:8080` | 10.0.2.2 é o localhost do computador visto pelo emulador |
| Aparelho físico via Wi-Fi | `http://IP_DO_PC:8080` (padrão do app: `http://192.168.0.21:8080`) | mesmo Wi-Fi do computador; libere a porta 8080 no firewall do Windows |
| Aparelho físico via USB | `http://localhost:8080` | rode antes `adb reverse tcp:8080 tcp:8080` |

O tráfego é HTTP puro; por isso o manifest usa `usesCleartextTraffic="true"`.

## Compilar e testar

```
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest
```

Requer o Android SDK (plataforma 37) e um JDK 17+ (testado com o JBR do Android Studio). Crie `local.properties` com `sdk.dir=...`.

Versões: Gradle 9.6.0, AGP 9.5.1, Kotlin 2.4.21 (plugin do Compose), Compose BOM 2026.09.00, minSdk 28, compileSdk 37.

## Uso do Lineup

- Toque numa barra de navio: abre o diálogo para editar ou excluir.
- Arraste o corpo da barra: move o navio (em dias inteiros).
- Arraste a borda esquerda ou direita: redimensiona a chegada ou a saída.
- Se a nova posição colidir com outro navio ou cruzar dia de maré baixa, a barra volta e um alerta é exibido.
