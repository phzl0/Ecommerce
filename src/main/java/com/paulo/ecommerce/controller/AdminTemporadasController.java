package com.paulo.ecommerce.controller;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import com.paulo.ecommerce.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/admin/temporadas")
public class AdminTemporadasController {
 private final TemporadaEditorService editor; private final PasseTemporadaRepository temporadas;private final ProdutoRepository produtos;private final PasseMissaoTemplateRepository missoes;
 public AdminTemporadasController(TemporadaEditorService e,PasseTemporadaRepository t,ProdutoRepository p,PasseMissaoTemplateRepository m){editor=e;temporadas=t;produtos=p;missoes=m;}
 @GetMapping @Transactional public String lista(Model m){m.addAttribute("temporadas",temporadas.findAllByOrderByInicioEmDesc());m.addAttribute("proximo",YearMonth.now().plusMonths(1));return "admin/temporadas/lista";}
 @GetMapping("/{id}") @Transactional public String editar(@PathVariable Long id,Model m){var t=temporadas.findById(id).orElseThrow();m.addAttribute("temporada",t);m.addAttribute("previa",editor.previa(id));m.addAttribute("produtos",produtos.findByTipoOrderByNomeAsc(Produto.Tipo.FISICO));m.addAttribute("missoes",missoes.findByTemporadaIdAndAtivaTrueOrderByPeriodicidadeAscOrdemExibicaoAsc(id));return "admin/temporadas/editor";}
 @PostMapping public String criar(@RequestParam String nome,@RequestParam LocalDate inicio,@RequestParam LocalDate fim,@RequestParam BigDecimal preco,@RequestParam int xp,@RequestParam int gratuitos,@RequestParam int premium,RedirectAttributes f){try{var t=editor.criar(nome,inicio,fim,preco,xp,gratuitos,premium);return "redirect:/admin/temporadas/"+t.getId();}catch(IllegalArgumentException|IllegalStateException ex){f.addFlashAttribute("erro",ex.getMessage());return "redirect:/admin/temporadas";}}
 private String executar(Long id,RedirectAttributes f,Runnable acao,String msg){try{acao.run();f.addFlashAttribute("msg",msg);}catch(IllegalArgumentException|IllegalStateException ex){f.addFlashAttribute("erro",ex.getMessage());}return "redirect:/admin/temporadas/"+id;}
 @PostMapping("/{id}/salvar") public String salvar(@PathVariable Long id,@RequestParam String nome,@RequestParam LocalDate inicio,@RequestParam LocalDate fim,@RequestParam BigDecimal preco,@RequestParam int xp,@RequestParam int gratuitos,@RequestParam int premium,RedirectAttributes f){return executar(id,f,()->editor.salvar(id,nome,inicio,fim,preco,xp,gratuitos,premium),"Rascunho salvo.");}
 @PostMapping("/{id}/premio") public String premio(@PathVariable Long id,@RequestParam int nivel,@RequestParam(defaultValue="false") boolean premium,@RequestParam String titulo,@RequestParam String descricao,@RequestParam String tipo,RedirectAttributes f){return executar(id,f,()->editor.premio(id,nivel,premium,titulo,descricao,tipo),"Recompensa salva.");}
 @PostMapping("/{id}/exemplos") public String exemplos(@PathVariable Long id,RedirectAttributes f){return executar(id,f,()->editor.exemplos(id),"Trilha demonstrativa criada. Revise os prêmios antes de publicar.");}
 @PostMapping("/{id}/opcao") public String opcao(@PathVariable Long id,@RequestParam Long recompensaId,@RequestParam Long produtoId,@RequestParam BigDecimal custo,@RequestParam BigDecimal frete,RedirectAttributes f){return executar(id,f,()->editor.opcao(id,recompensaId,produtoId,custo,frete),"Opção e custos salvos.");}
 @PostMapping("/{id}/opcao/{opcao}/remover") public String remover(@PathVariable Long id,@PathVariable Long opcao,RedirectAttributes f){return executar(id,f,()->editor.removerOpcao(id,opcao),"Opção removida.");}
 @PostMapping("/{id}/missao") public String missao(@PathVariable Long id,@RequestParam String titulo,@RequestParam PasseAcaoTipo acao,@RequestParam PassePeriodicidade periodo,@RequestParam int meta,@RequestParam int xp,@RequestParam(defaultValue="false") boolean premium,RedirectAttributes f){return executar(id,f,()->editor.missao(id,titulo,acao,periodo,meta,xp,premium),"Missão criada.");}
 @PostMapping("/{id}/missao/{missao}/remover") public String removerMissao(@PathVariable Long id,@PathVariable Long missao,RedirectAttributes f){return executar(id,f,()->editor.removerMissao(id,missao),"Missão removida.");}
 @PostMapping("/{id}/publicar") public String publicar(@PathVariable Long id,Principal principal,@RequestParam(defaultValue="false") boolean ciente,RedirectAttributes f){return executar(id,f,()->{if(!ciente)throw new IllegalArgumentException("Confirme a revisão das regras, dos custos e do estoque.");editor.publicar(id,principal.getName());},"Temporada publicada. As regras foram protegidas contra alterações.");}
 @PostMapping("/{id}/encerrar") public String encerrar(@PathVariable Long id,RedirectAttributes f){return executar(id,f,()->editor.encerrar(id),"Temporada encerrada. Prêmios já resgatados continuam no Baú.");}
}
