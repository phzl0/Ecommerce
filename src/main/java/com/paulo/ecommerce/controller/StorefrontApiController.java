package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.dto.store.StoreCatalogResponse;
import com.paulo.ecommerce.dto.store.StoreHomeResponse;
import com.paulo.ecommerce.dto.store.StoreProductDetailResponse;
import com.paulo.ecommerce.dto.store.StoreReviewRequest;
import com.paulo.ecommerce.service.AvaliacaoService;
import com.paulo.ecommerce.service.StorefrontService;
import java.math.BigDecimal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store")
public class StorefrontApiController {

  private final StorefrontService storefrontService;
  private final AvaliacaoService avaliacaoService;

  public StorefrontApiController(
      StorefrontService storefrontService, AvaliacaoService avaliacaoService) {
    this.storefrontService = storefrontService;
    this.avaliacaoService = avaliacaoService;
  }

  @GetMapping("/home")
  public StoreHomeResponse home() {
    return storefrontService.carregarHome();
  }

  @GetMapping("/catalog")
  public StoreCatalogResponse catalog(
      @RequestParam(required = false) String busca,
      @RequestParam(required = false) Long categoriaId,
      @RequestParam(required = false) BigDecimal precoMin,
      @RequestParam(required = false) BigDecimal precoMax,
      @RequestParam(defaultValue = "relevancia") String ordenacao) {
    return storefrontService.carregarCatalogo(busca, categoriaId, precoMin, precoMax, ordenacao);
  }

  @GetMapping("/products/{id}")
  public StoreProductDetailResponse produto(@PathVariable Long id) {
    return storefrontService.carregarProduto(id);
  }

  @PostMapping("/products/{id}/reviews")
  public StoreProductDetailResponse avaliar(
      @PathVariable Long id,
      @jakarta.validation.Valid @RequestBody StoreReviewRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    avaliacaoService.avaliar(id, userDetails.getUsername(), request.nota(), request.comentario());
    return storefrontService.carregarProduto(id);
  }
}
