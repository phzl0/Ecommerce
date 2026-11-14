package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.dto.CheckoutForm;
import com.paulo.ecommerce.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class FidelidadeApiController {
  private final FidelidadeService vip;
  private final BauService bau;
  public FidelidadeApiController(FidelidadeService vip, BauService bau) {
    this.vip = vip;
    this.bau = bau;
  }
  @GetMapping("/vip")
  public Object vip(@AuthenticationPrincipal UserDetails user) { return vip.perfil(user.getUsername()); }
  @PostMapping("/vip/{id}/claim")
  public Object claim(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
    return java.util.Map.of("itemId", vip.resgatar(user.getUsername(), id));
  }
  @GetMapping("/bau")
  public Object bau(@AuthenticationPrincipal UserDetails user) { return bau.listar(user.getUsername()); }
  @PostMapping("/bau/{id}/entrega")
  public Object entrega(@AuthenticationPrincipal UserDetails user, @PathVariable Long id,
      @Valid @RequestBody CheckoutForm form) {
    return java.util.Map.of("pedidoId", bau.solicitar(user.getUsername(), id, form));
  }
}
