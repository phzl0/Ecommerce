package com.paulo.ecommerce.dto.store;

import java.math.BigDecimal;

public record StoreProductCardResponse(
        Long id,
        String nome,
        String sku,
        String marca,
        String descricaoCurta,
        String imagemUrl,
        BigDecimal preco,
        Integer estoque,
        boolean destaque,
        StoreCategoryResponse categoria
) {
}
