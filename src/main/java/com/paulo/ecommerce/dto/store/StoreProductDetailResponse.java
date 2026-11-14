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
        List<StoreReviewResponse> avaliacoes,
        String tipo, Long familiaId, String opcao, List<Variacao> variacoes, List<String> imagens,
        BigDecimal pesoKg, BigDecimal comprimentoCm, BigDecimal larguraCm, BigDecimal alturaCm,
        int xpExtra, java.time.LocalDateTime xpExtraFim
) {
 public record Variacao(Long id,String sku,String opcao,BigDecimal preco,int estoque,String imagemUrl) {}
}
