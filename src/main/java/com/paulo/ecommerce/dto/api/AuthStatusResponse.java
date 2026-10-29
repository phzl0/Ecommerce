package com.paulo.ecommerce.dto.api;

public record AuthStatusResponse(
        boolean authenticated,
        String nome,
        String email,
        String tema,
        String role
) {
}
