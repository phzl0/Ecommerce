package com.paulo.ecommerce.controller;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import com.paulo.ecommerce.service.*;
import java.security.Principal;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin")
public class ExpedicaoController {
 @org.springframework.beans.factory.annotation.Autowired private PedidoDevolucaoRepository devolucoes;
 @org.springframework.beans.factory.annotation.Autowired private PedidoAtendimentoRepository mensagens;
 private final ExpedicaoService expedicao;
 private final PosVendaService posVenda;
 private final PedidoRepository pedidos;
 private final PedidoExpedicaoRepository envios;
 public ExpedicaoController(ExpedicaoService e,PosVendaService v,PedidoRepository p,PedidoExpedicaoRepository r) {expedicao=e;posVenda=v;pedidos=p;envios=r;}
 public record Fila(Pedido pedido,String etapa) {}
 @GetMapping("/expedicao")
 public String fila(Model model,@RequestParam(defaultValue="NA_FILA") String etapa,@RequestParam(defaultValue="0") int pagina) {
  if(!Set.of("NA_FILA","SEPARANDO","CONFERIDO","ENVIADO","ENTREGUE","").contains(etapa))throw new IllegalArgumentException("Etapa inválida.");
  var page=pedidos.filaExpedicao(etapa,org.springframework.data.domain.PageRequest.of(Math.max(0,pagina),20,org.springframework.data.domain.Sort.by("dataPedido","id")));
  model.addAttribute("fila",page.getContent().stream().map(p->new Fila(p,envios.findById(p.getId()).map(e->e.etapa).orElse("NA_FILA"))).toList());
  model.addAttribute("pagina",page);model.addAttribute("etapa",etapa);return "admin/expedicao";
 }
 @GetMapping("/atendimento")
 public String atendimento(Model model,@RequestParam(defaultValue="SOLICITADA") String estado,@RequestParam(defaultValue="0") int pagina) {
  if(!Set.of("SOLICITADA","AUTORIZADA","RECEBIDA","CONCLUIDA","RECUSADA","").contains(estado))throw new IllegalArgumentException("Estado inválido.");
  var page=devolucoes.fila(estado,org.springframework.data.domain.PageRequest.of(Math.max(0,pagina),20));
  model.addAttribute("solicitacoes",page.getContent());model.addAttribute("pagina",page);model.addAttribute("estado",estado);
  model.addAttribute("mensagens",mensagens.findTop50ByClienteTrueOrderByIdDesc());return "admin/atendimento";
 }
 private String executar(Long id,RedirectAttributes flash,Runnable tarefa,String sucesso) {
  try{tarefa.run();flash.addFlashAttribute("msg",sucesso);}catch(IllegalArgumentException|IllegalStateException e){flash.addFlashAttribute("erro",e.getMessage());}
  return "redirect:/admin/pedidos/"+id;
 }
 @PostMapping("/pedidos/{id}/separar")
 public String separar(@PathVariable Long id,RedirectAttributes f){return executar(id,f,()->expedicao.iniciar(id),"Separação iniciada.");}
 @PostMapping("/pedidos/{id}/conferir")
 public String conferir(@PathVariable Long id,@RequestParam Map<String,String> campos,Principal autor,RedirectAttributes f){
  return executar(id,f,()->{
   var quantidades=new HashMap<Long,Integer>();campos.forEach((k,v)->{if(k.startsWith("item_"))quantidades.put(Long.parseLong(k.substring(5)),Integer.parseInt(v));});
   expedicao.conferir(id,quantidades,autor.getName());
  },"Conferência concluída.");
 }
 @PostMapping("/pedidos/{id}/enviar")
 public String enviar(@PathVariable Long id,@RequestParam String transportadora,@RequestParam String codigo,@RequestParam(defaultValue="") String url,RedirectAttributes f){return executar(id,f,()->expedicao.enviar(id,transportadora,codigo,url),"Envio registrado.");}
 @PostMapping("/pedidos/{id}/rastreio")
 public String rastreio(@PathVariable Long id,@RequestParam String transportadora,@RequestParam String codigo,@RequestParam(defaultValue="") String url,RedirectAttributes f){return executar(id,f,()->expedicao.rastreio(id,transportadora,codigo,url),"Rastreamento atualizado.");}
 @PostMapping("/pedidos/{id}/entregar")
 public String entregar(@PathVariable Long id,RedirectAttributes f){return executar(id,f,()->expedicao.entregar(id),"Entrega registrada.");}
 @PostMapping("/pedidos/{id}/atendimento")
 public String nota(@PathVariable Long id,@RequestParam String mensagem,@RequestParam String chave,@RequestParam(defaultValue="false") boolean publico,Principal autor,RedirectAttributes f){return executar(id,f,()->posVenda.mensagemAdmin(id,autor.getName(),mensagem,chave,publico),publico?"Resposta enviada ao atendimento do cliente.":"Observação interna registrada.");}
 @PostMapping("/pedidos/{id}/solicitacoes/{solicitacao}")
 public String solicitacao(@PathVariable Long id,@PathVariable Long solicitacao,@RequestParam String acao,@RequestParam(defaultValue="0") int repor,@RequestParam String resposta,Principal autor,RedirectAttributes f){return executar(id,f,()->posVenda.atualizar(id,solicitacao,acao,repor,resposta,autor.getName()),"Solicitação atualizada.");}
}
