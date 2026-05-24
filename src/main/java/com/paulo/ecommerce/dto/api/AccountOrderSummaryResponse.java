package com.paulo.ecommerce.dto.api;

import java.math.BigDecimal;

public record AccountOrderSummaryResponse(
        Long id,
        String numero,
        String status,
        BigDecimal total,
        BigDecimal subtotal,
        BigDecimal frete,
        String dataPedido,
        int totalItens,
        String pagamentoStatus
) {
}
