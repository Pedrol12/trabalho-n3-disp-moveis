package br.edu.unirv.lineup.data.model

import androidx.compose.ui.graphics.Color

/** Categorias de carga do porto. O nome da constante é o mesmo enviado pelo backend. */
enum class CargoCategory(
    val rotulo: String,
    val nomeCompleto: String,
    val corHex: String
) {
    SOLID_VEG("Granéis Sólidos Vegetais", "Granéis sólidos – Vegetais (soja, milho, farelo, trigo)", "#16a34a"),
    CONTAINERS("Contêineres", "Carga Geral – Contêineres", "#0284c7"),
    PULP("Celulose", "Carga geral – Celulose", "#0d9488"),
    LIQUID_CHEM("Líquidos Químicos/Combustíveis", "Granéis líquidos – Químicos/Combustíveis", "#ea580c"),
    STEEL("Produtos Siderúrgicos", "Carga geral – Produtos siderúrgicos", "#475569"),
    MINERAL("Granéis Minerais", "Granéis sólidos – Minerais de desembarque", "#ca8a04"),
    CITRUS("Sucos Cítricos", "Granéis líquidos – Sucos cítricos", "#f59e0b"),
    MULTIPURPOSE("Multipropósito", "Berços públicos multipropósito", "#6366f1"),
    VEHICLES("Veículos (Ro-Ro)", "Carga Geral – Veículos", "#8b5cf6"),
    PASSENGERS("Passageiros (Cruzeiros)", "Passageiros", "#ec4899"),
    OFFSHORE("Carga Offshore", "Carga offshore", "#06b6d4");

    val cor: Color get() = corDeHex(corHex)
}

/** Converte "#rrggbb" em Color sem depender de android.graphics (testável em JVM puro). */
fun corDeHex(hex: String, padrao: Color = Color(0xFF64748B)): Color {
    val limpo = hex.trim().removePrefix("#")
    if (limpo.length != 6) return padrao
    val valor = limpo.toLongOrNull(16) ?: return padrao
    return Color(0xFF000000L or valor)
}
