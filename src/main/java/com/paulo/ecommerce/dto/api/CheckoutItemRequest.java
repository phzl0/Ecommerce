package com.paulo.ecommerce.dto.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutItemRequest(
    @NotNull Long produtoId,
    @NotNull @Min(1) @jakarta.validation.constraints.Max(999) Integer quantidade) {}
