package com.paulo.ecommerce.dto.api;

public record PasseAcaoRequest(
        String acao,
        Integer valor,
        String referencia
) {
}
