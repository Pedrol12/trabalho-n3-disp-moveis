package br.edu.unirv.lineup.data.local

import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.CargoCategory
import br.edu.unirv.lineup.data.model.PortCargo
import br.edu.unirv.lineup.data.model.RegisteredVessel
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.StatusNavio

/**
 * Dados mínimos de contingência usados quando o backend não responde.
 * Não reproduzem o banco do professor: são apenas uma amostra coerente para o app funcionar offline.
 * As janelas dos navios respeitam a maré baixa de novembro/2026 (dias 11, 12, 15, 16, 23, 24 e 30).
 */
object SantosPortDefaultData {

    private var proximoIdLocal = -1L

    private fun navio(
        berthId: String, nome: String, descarga: String, carga: String,
        ton: Int, ini: Double, fim: Double, status: String
    ) = ShipSchedule(
        id = proximoIdLocal--,
        berthId = berthId,
        shipName = nome,
        unloadingProduct = descarga,
        loadingProduct = carga,
        tonnage = ton,
        startDay = ini,
        endDay = fim,
        stripeColorHex = StatusNavio.corHex(status),
        status = status,
        durationDays = fim - ini
    )

    fun bercos(): List<Berth> {
        proximoIdLocal = -1L
        return listOf(
            Berth(
                "SV-01", "Berço SV-01", "Corredor de Exportação / Guarujá", "14.2m", CargoCategory.SOLID_VEG, 1,
                listOf(
                    navio("SV-01", "MV Santos Grain", "Nenhum (Lastro)", "Soja em Grãos", 60000, 1.0, 4.0, StatusNavio.OPERANDO),
                    navio("SV-01", "MV Paranaguá Star", "Nenhum (Lastro)", "Milho Granel", 48000, 5.0, 9.0, StatusNavio.CONFIRMADO)
                )
            ),
            Berth(
                "SV-02", "Berço SV-02", "Corredor de Exportação / Guarujá", "14.2m", CargoCategory.SOLID_VEG, 2,
                listOf(navio("SV-02", "MV Cerrado", "Nenhum (Lastro)", "Açúcar VHP", 42000, 2.0, 5.0, StatusNavio.PROGRAMADO))
            ),
            Berth(
                "CT-01", "Terminal de Contêineres 01", "Alamoa / Margem Esquerda", "15.0m", CargoCategory.CONTAINERS, 3,
                listOf(
                    navio("CT-01", "MSC Atlântico", "Contêineres Full", "Contêineres Reefer", 28000, 1.0, 3.0, StatusNavio.PRIORIDADE),
                    navio("CT-01", "Maersk Itapoá", "Contêineres Full", "Contêineres Full", 24000, 17.0, 20.0, StatusNavio.PROGRAMADO)
                )
            ),
            Berth(
                "CL-01", "Berço Celulose 01", "Paquetá / Outeirinhos", "13.8m", CargoCategory.PULP, 4,
                listOf(navio("CL-01", "Suzano Maranhão", "Nenhum (Lastro)", "Celulose Fibra Curta", 44000, 6.0, 8.0, StatusNavio.CONFIRMADO))
            ),
            Berth(
                "LQ-01", "Berço Granéis Líquidos 01", "Ilha Barnabé / Alamoa", "13.2m", CargoCategory.LIQUID_CHEM, 5,
                listOf(navio("LQ-01", "Brasil Tanker", "Diesel S10", "Nenhum", 36000, 3.0, 6.0, StatusNavio.OPERANDO))
            ),
            Berth(
                "SD-01", "Berço Siderúrgico 01", "Ponta da Praia / Saboó", "14.0m", CargoCategory.STEEL, 6, emptyList()
            ),
            Berth(
                "MN-01", "Berço Fertilizantes 01", "Ilha Barnabé", "13.0m", CargoCategory.MINERAL, 7,
                listOf(navio("MN-01", "Nordic Potash", "Cloreto de Potássio", "Nenhum", 32000, 25.0, 29.0, StatusNavio.PROGRAMADO))
            ),
            Berth(
                "PX-01", "Terminal de Passageiros", "Concais / Valongo", "10.5m", CargoCategory.PASSENGERS, 8,
                listOf(navio("PX-01", "Costa Fascinosa", "Suprimentos", "Bagagem", 3000, 13.0, 14.0, StatusNavio.CONFIRMADO))
            )
        ).map { b -> b.copy(schedules = b.schedules.map { it.copy(berthName = b.name) }) }
    }

    fun navios(): List<RegisteredVessel> = listOf(
        RegisteredVessel("IMO-9800001", "MV Santos Grain", "Graneleiro Panamax", "13.5", 60000, "Nenhum (Lastro)", "Soja em Grãos"),
        RegisteredVessel("IMO-9800002", "MSC Atlântico", "Porta-contêineres", "14.5", 28000, "Contêineres Full", "Contêineres Reefer"),
        RegisteredVessel("IMO-9800003", "Suzano Maranhão", "Graneleiro Handymax", "12.8", 44000, "Nenhum (Lastro)", "Celulose Fibra Curta"),
        RegisteredVessel("IMO-9800004", "Brasil Tanker", "Petroleiro Product", "12.0", 36000, "Diesel S10", "Nenhum"),
        RegisteredVessel("IMO-9800005", "Costa Fascinosa", "Cruzeiro", "8.5", 3000, "Suprimentos", "Bagagem")
    )

    fun cargas(): List<PortCargo> = listOf(
        PortCargo("CRG-01", "Soja em Grãos (Exportação)", "Carga", CargoCategory.SOLID_VEG, "36.000 t/dia"),
        PortCargo("CRG-02", "Milho Granel Exportação", "Carga", CargoCategory.SOLID_VEG, "34.000 t/dia"),
        PortCargo("CRG-03", "Açúcar VHP / Cristal", "Carga", CargoCategory.SOLID_VEG, "28.000 t/dia"),
        PortCargo("CRG-05", "Cloreto de Potássio / MAP (Fertilizante)", "Descarga", CargoCategory.MINERAL, "16.000 t/dia"),
        PortCargo("CRG-07", "Contêineres Full / Reefer (TEUs)", "Descarga & Carga", CargoCategory.CONTAINERS, "2.800 mov/dia"),
        PortCargo("CRG-08", "Celulose Fibra Curta (Fardos)", "Carga", CargoCategory.PULP, "22.000 t/dia"),
        PortCargo("CRG-09", "Diesel S10 / Gasolina / GLP / Químicos", "Descarga & Carga", CargoCategory.LIQUID_CHEM, "24.000 t/dia"),
        PortCargo("CRG-11", "Bobinas, Chapas e Produtos Siderúrgicos", "Descarga & Carga", CargoCategory.STEEL, "12.500 t/dia"),
        PortCargo("CRG-14", "Bagagem e Suprimento de Cruzeiros", "Descarga & Carga", CargoCategory.PASSENGERS, "4.500 pax/dia")
    )
}
