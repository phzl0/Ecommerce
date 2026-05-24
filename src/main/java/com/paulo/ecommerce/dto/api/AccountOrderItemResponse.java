package com.paulo.ecommerce.dto.api;

import java.math.BigDecimal;

public record AccountOrderItemResponse(
        Long produtoId,
        String nome,
        String imagemUrl,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
}
