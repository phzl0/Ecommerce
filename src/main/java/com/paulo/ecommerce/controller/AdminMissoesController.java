package com.paulo.ecommerce.controller;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import com.paulo.ecommerce.service.*;
import java.time.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/admin/temporadas/{temporada}/missoes")
public class AdminMissoesController {
 private final MissaoGestaoService gestao;private final PasseTemporadaRepository temporadas;private final PasseMissaoTemplateRepository missoes;private final ProdutoRepository produtos;private final CategoriaRepository categorias;private final EventoSemanalRepository eventos;
 public AdminMissoesController(MissaoGestaoService g,PasseTemporadaRepository t,PasseMissaoTemplateRepository m,ProdutoRepository p,CategoriaRepository c,EventoSemanalRepository e){gestao=g;temporadas=t;missoes=m;produtos=p;categorias=c;eventos=e;}
 @GetMapping @Transactional public String pagina(@PathVariable Long temporada,@RequestParam(required=false) Long editar,@RequestParam(required=false) LocalDate entrada,Model m){var t=temporadas.findById(temporada).orElseThrow();m.addAttribute("temporada",t);m.addAttribute("missoes",missoes.findByTemporadaIdOrderByOrdemExibicaoAscIdAsc(temporada));m.addAttribute("missao",editar==null?new PasseMissaoTemplate():gestao.buscar(temporada,editar));m.addAttribute("produtos",produtos.findAll());m.addAttribute("categorias",categorias.findAll());m.addAttribute("eventos",eventos.findTop30ByOrderByInicioDesc());
  var inicio=entrada==null?t.getInicioEm():entrada;m.addAttribute("entrada",inicio);try{m.addAttribute("simulacao",gestao.simular(temporada,inicio));}catch(IllegalArgumentException ex){m.addAttribute("erroSimulacao",ex.getMessage());}return "admin/temporadas/missoes";}
 private String executar(Long t,RedirectAttributes f,Runnable acao,String msg){try{acao.run();f.addFlashAttribute("msg",msg);}catch(IllegalArgumentException|IllegalStateException ex){f.addFlashAttribute("erro",ex.getMessage());}return "redirect:/admin/temporadas/"+t+"/missoes";}
 @PostMapping("/salvar") public String salvar(@PathVariable Long temporada,@RequestParam(required=false) Long id,@RequestParam String titulo,@RequestParam String descricao,@RequestParam PasseAcaoTipo acao,@RequestParam PassePeriodicidade periodo,@RequestParam int meta,@RequestParam int xp,@RequestParam(defaultValue="false") boolean premium,@RequestParam(defaultValue="false") boolean especial,@RequestParam int ordem,@RequestParam(required=false) Long produto,@RequestParam(required=false) Long categoria,@RequestParam(required=false) Long evento,@RequestParam(required=false) LocalDate inicio,@RequestParam(required=false) LocalDate fim,RedirectAttributes f){return executar(temporada,f,()->gestao.salvar(temporada,id,titulo,descricao,acao,periodo,meta,xp,premium,especial,ordem,produto,categoria,evento,inicio,fim),"Missão salva. Progresso publicado não foi alterado.");}
 @PostMapping("/{id}/duplicar") public String duplicar(@PathVariable Long temporada,@PathVariable Long id,RedirectAttributes f){return executar(temporada,f,()->gestao.duplicar(temporada,id),"Cópia criada no rascunho.");}
 @PostMapping("/{id}/ordem") public String ordem(@PathVariable Long temporada,@PathVariable Long id,@RequestParam int ordem,RedirectAttributes f){return executar(temporada,f,()->gestao.ordem(temporada,id,ordem),"Ordem de exibição salva.");}
}
