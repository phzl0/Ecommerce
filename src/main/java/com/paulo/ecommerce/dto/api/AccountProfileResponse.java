package com.paulo.ecommerce.dto.api;

public record AccountProfileResponse(
        Long id,
        String nome,
        String email,
        String role,
        int totalPedidos
) {
}
