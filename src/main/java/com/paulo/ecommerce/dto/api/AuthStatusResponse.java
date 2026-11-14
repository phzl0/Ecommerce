package com.paulo.ecommerce.dto.api;

public record AuthStatusResponse(
    boolean authenticated,
    Long id,
    String nome,
    String email,
    String tema,
    Long avatarPasse,
    Long badgePasse,
    String role,
    boolean ambienteLocal) {}
