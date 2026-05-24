package com.paulo.ecommerce.dto.store;

public record StoreReviewResponse(
        String autor,
        Integer nota,
        String comentario,
        String criadoEm
) {
}
