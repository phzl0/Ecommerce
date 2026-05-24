package com.paulo.ecommerce.dto.store;

import java.math.BigDecimal;
import java.util.List;

public record StoreProductDetailResponse(
        Long id,
        String nome,
        String sku,
        String marca,
        String descricaoCurta,
        String descricao,
        String imagemUrl,
        BigDecimal preco,
        Integer estoque,
        boolean destaque,
        Double mediaAvaliacoes,
        long totalAvaliacoes,
        StoreCategoryResponse categoria,
        List<StoreProductCardResponse> relacionados,
        List<StoreReviewResponse> avaliacoes
) {
}
