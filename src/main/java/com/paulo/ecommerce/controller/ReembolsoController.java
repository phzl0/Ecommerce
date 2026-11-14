package com.paulo.ecommerce.controller;
import com.paulo.ecommerce.service.*;
import com.paulo.ecommerce.repository.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin")
public class ReembolsoController {
 private final ReembolsoService service;private final PedidoService pedidos;private final PedidoReembolsoRepository repo;private final TrocaService trocas;
 public ReembolsoController(ReembolsoService s,PedidoService p,PedidoReembolsoRepository r,TrocaService t){service=s;pedidos=p;repo=r;trocas=t;}
 @GetMapping("/reembolsos") public String fila(Model m,@RequestParam(defaultValue="PENDENTE") String estado,@RequestParam(defaultValue="false") boolean analise,@RequestParam(defaultValue="0") int pagina){
  if(!Set.of("PENDENTE","CONFIRMADO","CANCELADO","").contains(estado))throw new IllegalArgumentException("Estado inválido.");
  var page=repo.fila(estado,analise,org.springframework.data.domain.PageRequest.of(Math.max(0,pagina),20));m.addAttribute("pagina",page);m.addAttribute("estado",estado);m.addAttribute("analise",analise);return "admin/reembolsos";
 }
 @GetMapping("/pedidos/{id}/reembolso") public String formulario(@PathVariable Long id,@RequestParam(required=false) BigDecimal valor,@RequestParam(required=false) Long confirmar,Model m){
  var pedido=pedidos.buscarPorId(id);m.addAttribute("pedido",pedido);m.addAttribute("chave",UUID.randomUUID().toString());m.addAttribute("local",service.simulacaoDisponivel());
  if(confirmar!=null){var r=repo.findById(confirmar).orElseThrow();if(!r.pedidoId.equals(id)||!"PENDENTE".equals(r.estado))throw new IllegalArgumentException("Reembolso não está pendente neste pedido.");valor=r.valor;m.addAttribute("reembolso",r);}
  m.addAttribute("valor",valor==null?pedido.getTotal().subtract(pedido.getValorReembolsado()):valor);
  if(valor!=null)try{m.addAttribute("aviso",service.prever(id,valor));}catch(IllegalStateException|IllegalArgumentException e){m.addAttribute("erro",e.getMessage());}
  return "admin/reembolso";
 }
 private String executar(Long id,RedirectAttributes f,Runnable r){try{r.run();f.addFlashAttribute("msg","Operação registrada.");}catch(IllegalArgumentException|IllegalStateException e){f.addFlashAttribute("erro",e.getMessage());}return "redirect:/admin/pedidos/"+id;}
 @PostMapping("/pedidos/{id}/reembolso") public String solicitar(@PathVariable Long id,@RequestParam BigDecimal valor,@RequestParam String motivo,@RequestParam String chave,@RequestParam String token,@RequestParam(defaultValue="false") boolean ciente,Principal u,RedirectAttributes f){return executar(id,f,()->service.solicitar(id,valor,motivo,chave,token,ciente,u.getName()));}
 @PostMapping("/pedidos/{id}/reembolsos/{reembolso}/confirmar") public String confirmar(@PathVariable Long id,@PathVariable Long reembolso,@RequestParam String token,@RequestParam(defaultValue="false") boolean ciente,Principal u,RedirectAttributes f){return executar(id,f,()->service.confirmarSimulado(id,reembolso,token,ciente,u.getName()));}
 @PostMapping("/pedidos/{id}/reembolsos/{reembolso}/cancelar") public String cancelar(@PathVariable Long id,@PathVariable Long reembolso,Principal u,RedirectAttributes f){return executar(id,f,()->service.cancelar(id,reembolso,u.getName()));}
 @PostMapping("/pedidos/{id}/reembolsos/{reembolso}/analise") public String analisar(@PathVariable Long id,@PathVariable Long reembolso,@RequestParam String resolucao,Principal u,RedirectAttributes f){return executar(id,f,()->service.analisar(id,reembolso,resolucao,u.getName()));}
 @PostMapping("/pedidos/{id}/solicitacoes/{solicitacao}/troca") public String trocar(@PathVariable Long id,@PathVariable Long solicitacao,@RequestParam Long produtoId,@RequestParam String motivo,@RequestParam(defaultValue="false") boolean ciente,Principal u,RedirectAttributes f){return executar(id,f,()->trocas.criar(id,solicitacao,produtoId,motivo,ciente,u.getName()));}
}
