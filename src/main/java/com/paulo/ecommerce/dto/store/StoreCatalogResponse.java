package com.paulo.ecommerce.dto.store;

import java.util.List;

public record StoreCatalogResponse(
        List<StoreCategoryResponse> categorias,
        List<StoreProductCardResponse> produtos,
        long total
) {
}
