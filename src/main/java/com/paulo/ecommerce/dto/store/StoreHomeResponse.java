package com.paulo.ecommerce.dto.store;

import java.util.List;

public record StoreHomeResponse(
        String brand,
        String headline,
        String subheadline,
        java.util.List<StoreBannerResponse> banners,
        List<StoreCategoryResponse> categorias,
        List<StoreProductCardResponse> destaques,
        List<StoreProductCardResponse> novidades
) {
}
