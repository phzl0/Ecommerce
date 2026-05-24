package com.paulo.ecommerce.dto.store;

public record StoreBannerResponse(
        String titulo,
        String subtitulo,
        String destaque,
        String ctaLabel,
        String ctaHref,
        String imagemUrl
) {
}
