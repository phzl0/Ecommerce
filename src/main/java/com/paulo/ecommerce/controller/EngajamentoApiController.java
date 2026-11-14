package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.service.EventoSemanalService;
import com.paulo.ecommerce.service.NotificacaoService;
import com.paulo.ecommerce.service.UsuarioService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EngajamentoApiController {
  private final UsuarioService usuarios;
  private final NotificacaoService notificacoes;
  private final EventoSemanalService eventos;

  public EngajamentoApiController(UsuarioService usuarios, NotificacaoService notificacoes,
      EventoSemanalService eventos) {
    this.usuarios = usuarios;
    this.notificacoes = notificacoes;
    this.eventos = eventos;
  }

  @GetMapping("/events")
  public List<EventoSemanalService.Visao> eventos(@AuthenticationPrincipal UserDetails user) {
    return eventos.listar(user == null ? null : usuarioId(user));
  }

  @PostMapping("/account/events/{id}/claim")
  public void resgatar(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
    eventos.resgatar(id, user.getUsername());
  }

  @GetMapping("/account/events/badges")
  public List<String> selos(@AuthenticationPrincipal UserDetails user) {
    return eventos.selos(usuarioId(user));
  }

  @GetMapping("/account/notifications")
  public NotificacaoService.Caixa notificacoes(@AuthenticationPrincipal UserDetails user) {
    return notificacoes.caixa(usuarioId(user));
  }

  @PostMapping("/account/notifications/read/{id}")
  public void marcarLidas(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
    notificacoes.marcarLidas(usuarioId(user), id);
  }

  private Long usuarioId(UserDetails user) {
    return usuarios.buscarPorEmail(user.getUsername()).getId();
  }
}
