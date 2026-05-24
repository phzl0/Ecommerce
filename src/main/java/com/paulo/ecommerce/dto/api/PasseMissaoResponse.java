package com.paulo.ecommerce.dto.api;

public record PasseMissaoResponse(
        Long id,
        String periodicidade,
        String titulo,
        String descricao,
        int progressoAtual,
        int metaValor,
        int xpRecompensa,
        boolean concluida,
        String resetaEm,
        String ctaLabel,
        String ctaHref
) {
}
