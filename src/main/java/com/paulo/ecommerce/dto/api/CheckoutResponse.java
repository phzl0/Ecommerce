package com.paulo.ecommerce.dto.api;

public record CheckoutResponse(
        Long pedidoId,
        String numeroPedido,
        String redirectUrl
) {
}
