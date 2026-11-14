package com.paulo.ecommerce.controller;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import com.paulo.ecommerce.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.*;
@Controller
@RequestMapping("/admin")
public class AdminOperacaoController {
 private final ProdutoRepository produtos;
 private final EstoqueReservaRepository reservas;
 private final EstoqueMovimentoRepository movimentos;
 private final DigitalConfigRepository configs;
 private final DigitalEntregaRepository entregas;
 private final DigitalCodigoRepository codigos;
 private final EstoqueService estoque;
 private final DigitalService digitais;
 public AdminOperacaoController(ProdutoRepository p,EstoqueReservaRepository r,EstoqueMovimentoRepository m,
     DigitalConfigRepository c,DigitalEntregaRepository e,DigitalCodigoRepository k,EstoqueService s,DigitalService d) {
  produtos=p;reservas=r;movimentos=m;configs=c;entregas=e;codigos=k;estoque=s;digitais=d;
 }
 public record Saldo(Produto produto,long pedidos,long bau) {}
 @GetMapping("/estoque")
 public String estoque(Model model,@RequestParam(defaultValue="") String busca,@RequestParam(defaultValue="0") int pagina,
     @RequestParam(required=false) Long produtoId) {
  var page=produtos.buscarCatalogoAdmin(busca,null,false,false,5,org.springframework.data.domain.PageRequest.of(Math.max(0,pagina),20,org.springframework.data.domain.Sort.by("nome")));
  model.addAttribute("saldos",page.getContent().stream().map(p->new Saldo(p,reservas.totalReservado(p.getId(),"PEDIDO:%"),reservas.totalReservado(p.getId(),"BAU:%"))).toList());
  model.addAttribute("pagina",page);model.addAttribute("busca",busca);
  model.addAttribute("selecionado",produtoId==null?null:produtos.findById(produtoId).orElseThrow());
  model.addAttribute("movimentos",produtoId==null?List.of():movimentos.findTop100ByProdutoIdOrderByIdDesc(produtoId));
  return "admin/estoque";
 }
 @PostMapping("/estoque/{id}/ajustar")
 public String ajustar(@PathVariable Long id,@RequestParam int quantidade,@RequestParam int minimo,@RequestParam String motivo,Principal principal,RedirectAttributes flash) {
  try {estoque.ajustar(id,quantidade,minimo,motivo,principal.getName());flash.addFlashAttribute("msg","Ajuste registrado.");}
  catch(IllegalArgumentException e) {flash.addFlashAttribute("erro",e.getMessage());}
  return "redirect:/admin/estoque?produtoId="+id;
 }
 @GetMapping("/digitais")
 public String digitais(Model model,@RequestParam(required=false) Long produtoId) {
  model.addAttribute("produtos",produtos.findByTipoOrderByNomeAsc(Produto.Tipo.DIGITAL).stream().filter(p->p.getSku()==null || !p.getSku().startsWith("PASSE-MENSAL-")).toList());
  model.addAttribute("selecionado",produtoId==null?null:produtos.findById(produtoId).orElseThrow());
  model.addAttribute("config",produtoId==null?null:configs.findById(produtoId).orElse(null));
  model.addAttribute("codigosLivres",produtoId==null?0:codigos.countByProdutoIdAndEstado(produtoId,"DISPONIVEL"));
  model.addAttribute("entregas",entregas.findTop100ByOrderByIdDesc());return "admin/digitais";
 }
 @PostMapping("/digitais/{id}/arquivo")
 public String arquivo(@PathVariable Long id,@RequestParam MultipartFile arquivo,RedirectAttributes flash) throws java.io.IOException {
  try {digitais.arquivo(id,arquivo.getOriginalFilename(),arquivo.getBytes());flash.addFlashAttribute("msg","Nova versão cadastrada. Compras anteriores mantêm seu arquivo original.");}
  catch(IllegalArgumentException e) {flash.addFlashAttribute("erro",e.getMessage());}
  return "redirect:/admin/digitais?produtoId="+id;
 }
 @PostMapping("/digitais/{id}/codigos")
 public String codigos(@PathVariable Long id,@RequestParam String valores,Principal principal,RedirectAttributes flash) {
  try {var n=digitais.importarCodigos(id,valores,principal.getName());flash.addFlashAttribute("msg",n+" códigos adicionados. Duplicados foram ignorados.");}
  catch(IllegalArgumentException e) {flash.addFlashAttribute("erro",e.getMessage());}
  return "redirect:/admin/digitais?produtoId="+id;
 }
}
