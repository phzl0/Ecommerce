package com.paulo.ecommerce.controller;
import com.paulo.ecommerce.service.PosVendaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/account/orders/{id}/operation")
public class PosVendaApiController {
 private final PosVendaService service;
 public PosVendaApiController(PosVendaService s){service=s;}
 @GetMapping public Map<String,Object> acompanhar(@PathVariable Long id,Principal u){return service.acompanhar(id,u.getName());}
 public record Mensagem(@NotBlank @Size(min=2,max=2000) String mensagem,@NotBlank @Size(min=8,max=72) String chave) {}
 @PostMapping("/messages") public Map<String,Boolean> mensagem(@PathVariable Long id,Principal u,@Valid @RequestBody Mensagem r){service.mensagemCliente(id,u.getName(),r.mensagem(),r.chave());return Map.of("ok",true);}
 public record Solicitacao(@NotNull Long itemId,@Min(1) int quantidade,@NotBlank String tipo,@NotBlank @Size(min=10,max=1000) String motivo,@NotBlank @Size(min=8,max=80) String chave,@NotBlank String token,boolean ciente) {}
 @GetMapping("/impact") public com.paulo.ecommerce.service.ImpactoReembolsoService.Aviso impacto(@PathVariable Long id,Principal u,@RequestParam Long itemId,@RequestParam int quantidade){return service.preverSolicitacao(id,u.getName(),itemId,quantidade);}
 @PostMapping("/requests") public Map<String,Long> solicitar(@PathVariable Long id,Principal u,@Valid @RequestBody Solicitacao r){return Map.of("id",service.solicitarComAviso(id,u.getName(),r.itemId(),r.quantidade(),r.tipo(),r.motivo(),r.chave(),r.token(),r.ciente()));}
}
