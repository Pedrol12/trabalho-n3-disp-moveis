package br.edu.unirv.lineup.data.remote

import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.BerthRequest
import br.edu.unirv.lineup.data.model.HighwayCargo
import br.edu.unirv.lineup.data.model.HighwayLine
import br.edu.unirv.lineup.data.model.HighwaySector
import br.edu.unirv.lineup.data.model.Locomotive
import br.edu.unirv.lineup.data.model.LocomotiveArrival
import br.edu.unirv.lineup.data.model.LocomotiveRequest
import br.edu.unirv.lineup.data.model.PeraHourlyState
import br.edu.unirv.lineup.data.model.PortCargo
import br.edu.unirv.lineup.data.model.RailAxisYard
import br.edu.unirv.lineup.data.model.RailTerminalBranch
import br.edu.unirv.lineup.data.model.Wagon
import br.edu.unirv.lineup.data.model.WagonAllocation
import br.edu.unirv.lineup.data.model.WagonRequest
import br.edu.unirv.lineup.data.model.RegisteredTruck
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.TruckSchedule
import br.edu.unirv.lineup.data.model.TruckScheduleRequest
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.ShipScheduleRequest
import br.edu.unirv.lineup.data.model.ShipScheduleWindowUpdateRequest
import br.edu.unirv.lineup.data.model.TideTable
import br.edu.unirv.lineup.data.model.TideTableSummary
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/** Erro HTTP devolvido pelo backend (400, 404, 409, 500...). */
class ApiException(val codigo: Int, mensagem: String) : Exception(mensagem)

/** Cliente HTTP do LineupBackend. Todas as chamadas são suspend e rodam em Dispatchers.IO. */
object BackendApiClient {

    @Volatile
    var baseUrl: String = BackendConfig.DEFAULT_NETWORK_URL

    private val gson = Gson()
    private val tipoJson = "application/json; charset=utf-8".toMediaType()

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // ---------------------------------------------------------------- infraestrutura

    private fun montarUrl(caminho: String, vararg segmentos: String): HttpUrl {
        val base = baseUrl.toHttpUrlOrNull() ?: throw IOException("URL do backend inválida: $baseUrl")
        val construtor = base.newBuilder().addPathSegments(caminho)
        segmentos.forEach { construtor.addPathSegment(it) }
        return construtor.build()
    }

    private fun extrairMensagem(corpo: String, codigo: Int): String {
        try {
            val json = JsonParser.parseString(corpo)
            if (json.isJsonObject) {
                val msg = (json as JsonObject).get("message")
                if (msg != null && msg.isJsonPrimitive) return msg.asString
            }
        } catch (_: Exception) {
        }
        return when (codigo) {
            400 -> "Requisição inválida (400)."
            404 -> "Registro não encontrado (404)."
            409 -> "Conflito com as regras operacionais (409)."
            else -> "Erro do servidor (HTTP $codigo)."
        }
    }

    private suspend fun <T> executar(
        metodo: String,
        url: HttpUrl,
        corpo: Any? = null,
        tipo: Type? = null
    ): T? = withContext(Dispatchers.IO) {
        val requestBody: RequestBody? = when {
            corpo != null -> gson.toJson(corpo).toRequestBody(tipoJson)
            metodo == "POST" || metodo == "PUT" || metodo == "PATCH" -> ByteArray(0).toRequestBody(null)
            else -> null
        }
        val requisicao = Request.Builder().url(url).method(metodo, requestBody)
            .header("Accept", "application/json").build()
        http.newCall(requisicao).execute().use { resposta ->
            val texto = resposta.body?.string().orEmpty()
            if (!resposta.isSuccessful) throw ApiException(resposta.code, extrairMensagem(texto, resposta.code))
            if (tipo == null || texto.isBlank()) {
                null
            } else {
                try {
                    gson.fromJson<T>(texto, tipo)
                } catch (e: JsonParseException) {
                    throw IOException("Resposta do servidor em formato inesperado.", e)
                }
            }
        }
    }

    private inline fun <reified T> tipoDe(): Type = object : TypeToken<T>() {}.type

    private suspend inline fun <reified T> obter(url: HttpUrl): T =
        executar<T>("GET", url, null, tipoDe<T>()) ?: throw IOException("Resposta vazia do servidor.")

    private suspend inline fun <reified T> enviar(metodo: String, url: HttpUrl, corpo: Any?): T =
        executar<T>(metodo, url, corpo, tipoDe<T>()) ?: throw IOException("Resposta vazia do servidor.")

    // ---------------------------------------------------------------- conexão

    /** GET /api/port/tides/active. Devolve true se respondeu 2xx; lança exceção caso contrário. */
    suspend fun checkConnection(): Boolean {
        executar<Any>("GET", montarUrl("api/port/tides/active"))
        return true
    }

    // ---------------------------------------------------------------- berços

    suspend fun listarBercos(): List<Berth> = obter(montarUrl("api/port/berths"))

    suspend fun criarBerco(req: BerthRequest): Berth = enviar("POST", montarUrl("api/port/berths"), req)

    suspend fun atualizarBerco(id: String, req: BerthRequest): Berth =
        enviar("PUT", montarUrl("api/port/berths", id), req)

    suspend fun excluirBerco(id: String) {
        executar<Any>("DELETE", montarUrl("api/port/berths", id))
    }

    // ---------------------------------------------------------------- atracações

    suspend fun listarAtracacoes(): List<ShipSchedule> = obter(montarUrl("api/port/schedules"))

    suspend fun criarAtracacao(req: ShipScheduleRequest): ShipSchedule =
        enviar("POST", montarUrl("api/port/schedules"), req)

    suspend fun atualizarAtracacao(id: Long, req: ShipScheduleRequest): ShipSchedule =
        enviar("PUT", montarUrl("api/port/schedules", id.toString()), req)

    /** PATCH /api/port/schedules/{id}/window */
    suspend fun moverAtracacao(id: Long, req: ShipScheduleWindowUpdateRequest): ShipSchedule =
        enviar("PATCH", montarUrl("api/port/schedules", id.toString(), "window"), req)

    suspend fun excluirAtracacao(id: Long) {
        executar<Any>("DELETE", montarUrl("api/port/schedules", id.toString()))
    }

    // ---------------------------------------------------------------- navios

    suspend fun listarNavios(): List<RegisteredVessel> = obter(montarUrl("api/port/vessels"))

    suspend fun criarNavio(navio: RegisteredVessel): RegisteredVessel =
        enviar("POST", montarUrl("api/port/vessels"), navio)

    suspend fun atualizarNavio(imo: String, navio: RegisteredVessel): RegisteredVessel =
        enviar("PUT", montarUrl("api/port/vessels", imo), navio)

    suspend fun excluirNavio(imo: String) {
        executar<Any>("DELETE", montarUrl("api/port/vessels", imo))
    }

    // ---------------------------------------------------------------- cargas

    suspend fun listarCargas(): List<PortCargo> = obter(montarUrl("api/port/cargos"))

    suspend fun criarCarga(carga: PortCargo): PortCargo = enviar("POST", montarUrl("api/port/cargos"), carga)

    suspend fun atualizarCarga(codigo: String, carga: PortCargo): PortCargo =
        enviar("PUT", montarUrl("api/port/cargos", codigo), carga)

    suspend fun excluirCarga(codigo: String) {
        executar<Any>("DELETE", montarUrl("api/port/cargos", codigo))
    }

    // ---------------------------------------------------------------- marés

    suspend fun listarMaresCompletas(): List<TideTable> = obter(montarUrl("api/port/tides/full"))

    suspend fun ativarMare(id: Long): TideTableSummary =
        enviar("POST", montarUrl("api/port/tides", id.toString(), "activate"), null)

    // ---------------------------------------------------------------- ferrovia

    /** GET /api/rail/pera/state?date=aaaa-mm-dd&hour=h */
    suspend fun obterEstadoPera(data: String, hora: Int): PeraHourlyState {
        val url = montarUrl("api/rail/pera/state").newBuilder()
            .addQueryParameter("date", data).addQueryParameter("hour", hora.toString()).build()
        return obter(url)
    }

    suspend fun listarRamais(): List<RailTerminalBranch> = obter(montarUrl("api/rail/branches"))

    /** PATCH /api/rail/branches/{code}/amv?active=... */
    suspend fun alternarAmv(codigo: String, ativo: Boolean): RailTerminalBranch {
        val url = montarUrl("api/rail/branches", codigo, "amv").newBuilder()
            .addQueryParameter("active", ativo.toString()).build()
        return enviar("PATCH", url, null)
    }

    suspend fun listarEixosPatios(): List<RailAxisYard> = obter(montarUrl("api/rail/axes-yards"))

    suspend fun listarLocomotivas(): List<Locomotive> = obter(montarUrl("api/rail/locomotives"))

    suspend fun criarLocomotiva(req: LocomotiveRequest): Locomotive =
        enviar("POST", montarUrl("api/rail/locomotives"), req)

    suspend fun atualizarLocomotiva(codigo: String, req: LocomotiveRequest): Locomotive =
        enviar("PUT", montarUrl("api/rail/locomotives", codigo), req)

    suspend fun excluirLocomotiva(codigo: String) {
        executar<Any>("DELETE", montarUrl("api/rail/locomotives", codigo))
    }

    suspend fun listarVagoes(): List<Wagon> = obter(montarUrl("api/rail/wagons"))

    suspend fun criarVagao(req: WagonRequest): Wagon = enviar("POST", montarUrl("api/rail/wagons"), req)

    suspend fun atualizarVagao(codigo: String, req: WagonRequest): Wagon =
        enviar("PUT", montarUrl("api/rail/wagons", codigo), req)

    suspend fun excluirVagao(codigo: String) {
        executar<Any>("DELETE", montarUrl("api/rail/wagons", codigo))
    }

    suspend fun listarAlocacoes(): List<WagonAllocation> = obter(montarUrl("api/rail/allocations"))

    suspend fun criarAlocacao(a: WagonAllocation): WagonAllocation =
        enviar("POST", montarUrl("api/rail/allocations"), a)

    suspend fun atualizarAlocacao(id: String, a: WagonAllocation): WagonAllocation =
        enviar("PUT", montarUrl("api/rail/allocations", id), a)

    suspend fun excluirAlocacao(id: String) {
        executar<Any>("DELETE", montarUrl("api/rail/allocations", id))
    }

    suspend fun listarChegadas(): List<LocomotiveArrival> = obter(montarUrl("api/rail/arrivals"))

    suspend fun criarChegada(c: LocomotiveArrival): LocomotiveArrival =
        enviar("POST", montarUrl("api/rail/arrivals"), c)

    suspend fun atualizarChegada(id: String, c: LocomotiveArrival): LocomotiveArrival =
        enviar("PUT", montarUrl("api/rail/arrivals", id), c)

    suspend fun excluirChegada(id: String) {
        executar<Any>("DELETE", montarUrl("api/rail/arrivals", id))
    }

    // ---------------------------------------------------------------- rodovia

    /** GET /api/highway/lines?includeTrucks=true (180 docas com seus agendamentos). */
    suspend fun listarDocas(): List<HighwayLine> {
        val url = montarUrl("api/highway/lines").newBuilder().addQueryParameter("includeTrucks", "true").build()
        return obter(url)
    }

    suspend fun listarSetores(): List<HighwaySector> = obter(montarUrl("api/highway/sectors"))

    /** POST /api/highway/schedules. O truckId é gerado pelo backend quando vem nulo. */
    suspend fun criarAgendamento(req: TruckScheduleRequest): TruckSchedule =
        enviar("POST", montarUrl("api/highway/schedules"), req)

    suspend fun atualizarAgendamento(truckId: String, req: TruckScheduleRequest): TruckSchedule =
        enviar("PUT", montarUrl("api/highway/schedules", truckId), req)

    suspend fun excluirAgendamento(truckId: String) {
        executar<Any>("DELETE", montarUrl("api/highway/schedules", truckId))
    }

    suspend fun listarCaminhoes(): List<RegisteredTruck> = obter(montarUrl("api/highway/trucks"))

    suspend fun criarCaminhao(c: RegisteredTruck): RegisteredTruck = enviar("POST", montarUrl("api/highway/trucks"), c)

    suspend fun atualizarCaminhao(codigo: String, c: RegisteredTruck): RegisteredTruck =
        enviar("PUT", montarUrl("api/highway/trucks", codigo), c)

    suspend fun excluirCaminhao(codigo: String) {
        executar<Any>("DELETE", montarUrl("api/highway/trucks", codigo))
    }

    suspend fun listarCargasRodoviarias(): List<HighwayCargo> = obter(montarUrl("api/highway/cargos"))

    suspend fun criarCargaRodoviaria(c: HighwayCargo): HighwayCargo = enviar("POST", montarUrl("api/highway/cargos"), c)

    suspend fun atualizarCargaRodoviaria(codigo: String, c: HighwayCargo): HighwayCargo =
        enviar("PUT", montarUrl("api/highway/cargos", codigo), c)

    suspend fun excluirCargaRodoviaria(codigo: String) {
        executar<Any>("DELETE", montarUrl("api/highway/cargos", codigo))
    }
}
