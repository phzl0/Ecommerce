package com.paulo.ecommerce.dto.api;

public record UpdateProfileRequest(
        String nome,
        String senha
) {
}
