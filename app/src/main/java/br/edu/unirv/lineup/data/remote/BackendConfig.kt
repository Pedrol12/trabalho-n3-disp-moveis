package br.edu.unirv.lineup.data.remote

import android.content.Context

/** URL base do LineupBackend, persistida em SharedPreferences. */
object BackendConfig {
    const val DEFAULT_NETWORK_URL = "http://192.168.0.21:8080"
    const val EMULATOR_URL = "http://10.0.2.2:8080"
    const val LOCAL_URL = "http://localhost:8080"

    private const val ARQUIVO_PREFS = "lineup_backend"
    private const val CHAVE_URL = "backend_url"

    fun carregarUrl(context: Context): String =
        context.getSharedPreferences(ARQUIVO_PREFS, Context.MODE_PRIVATE)
            .getString(CHAVE_URL, DEFAULT_NETWORK_URL) ?: DEFAULT_NETWORK_URL

    fun salvarUrl(context: Context, url: String) {
        context.getSharedPreferences(ARQUIVO_PREFS, Context.MODE_PRIVATE)
            .edit().putString(CHAVE_URL, url).apply()
    }

    /** Remove espaços e barra final, e assume http:// quando o esquema foi omitido. */
    fun normalizar(entrada: String): String {
        var url = entrada.trim().trimEnd('/')
        if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        return url
    }
}
