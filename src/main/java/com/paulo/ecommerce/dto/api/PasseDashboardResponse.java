package com.paulo.ecommerce.dto.api;

import java.util.List;

public record PasseDashboardResponse(
        boolean premiumAtivo,
        java.math.BigDecimal precoPremium,
        java.math.BigDecimal gastoAprovado,
        java.math.BigDecimal gastoMinimoFinal,
        String temporadaNome,
        String temporadaPeriodo,
        int xpTotal,
        int nivelAtual,
        int totalNiveis,
        int xpPorNivel,
        int xpNoNivelAtual,
        int xpParaProximoNivel,
        List<PasseNivelResponse> niveis,
        List<PasseMissaoResponse> missoesDiarias,
        List<PasseMissaoResponse> missoesSemanais,
        List<PasseMissaoResponse> missoesMensais,
        List<PasseHistoricoXpResponse> historico
) {
}
