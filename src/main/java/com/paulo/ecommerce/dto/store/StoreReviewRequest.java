package com.paulo.ecommerce.dto.store;

public record StoreReviewRequest(
    @jakarta.validation.constraints.NotNull
        @jakarta.validation.constraints.Min(1)
        @jakarta.validation.constraints.Max(5)
        Integer nota,
    @jakarta.validation.constraints.Size(max = 500) String comentario) {}
