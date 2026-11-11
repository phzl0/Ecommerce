package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import com.paulo.ecommerce.service.FidelidadeService;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/fidelidade")
public class AdminFidelidadeController {
  private final FidelidadeService fidelidade;
  private final FidelidadeConfigRepository configuracoes;
  private final VipNivelRepository niveis;
  private final ProdutoRepository produtos;
  private final PasseTemporadaRepository temporadas;
  private final PasseRecompensaNivelRepository recompensas;
  private final RecompensaOpcaoRepository opcoes;
  private final PasseMissaoTemplateRepository missoes;

  public AdminFidelidadeController(FidelidadeService fidelidade, FidelidadeConfigRepository configuracoes,
      VipNivelRepository niveis, ProdutoRepository produtos, PasseTemporadaRepository temporadas,
      PasseRecompensaNivelRepository recompensas, RecompensaOpcaoRepository opcoes,
      PasseMissaoTemplateRepository missoes) {
    this.fidelidade = fidelidade; this.configuracoes = configuracoes; this.niveis = niveis;
    this.produtos = produtos; this.temporadas = temporadas; this.recompensas = recompensas;
    this.opcoes = opcoes; this.missoes = missoes;
  }

  @GetMapping
  @Transactional
  public String pagina(Model model) {
    var mes = YearMonth.now();
    var temporada = temporadas.findByAnoAndMes(mes.getYear(), mes.getMonthValue()).orElseThrow();
    model.addAttribute("config", fidelidade.config());
    model.addAttribute("niveis", niveis.findAll());
    model.addAttribute("produtos", produtos.findAll().stream().filter(p -> p.getTipo() == Produto.Tipo.FISICO).toList());
    model.addAttribute("recompensas", recompensas.findByTemporadaIdOrderByNivelAsc(temporada.getId()));
    model.addAttribute("opcoes", opcoes.findAll());
    model.addAttribute("missoes", missoes.findByTemporadaIdAndAtivaTrueOrderByPeriodicidadeAscOrdemExibicaoAsc(temporada.getId()));
    return "admin/fidelidade";
  }

  @PostMapping("/config")
  @Transactional
  public String config(@RequestParam BigDecimal precoPasse, @RequestParam int diasInatividade,
      @RequestParam int pontosPorReal, @RequestParam int pontosMissao, @RequestParam int pontosEvento,
      @RequestParam int bonusPrimeiroPasse, @RequestParam int bonusPasseTemporada, RedirectAttributes flash) {
    if (precoPasse.signum() <= 0 || precoPasse.compareTo(new BigDecimal("9999.99")) > 0
        || precoPasse.scale() > 2 || diasInatividade < 1 || diasInatividade > 3650
        || pontosPorReal < 0 || pontosPorReal > 100 || pontosMissao < 0 || pontosMissao > 1000
        || pontosEvento < 0 || pontosEvento > 10000 || bonusPrimeiroPasse < 0 || bonusPrimeiroPasse > 10000
        || bonusPasseTemporada < 0 || bonusPasseTemporada > 10000) {
      flash.addFlashAttribute("erro", "Confira os limites dos valores informados.");
      return "redirect:/admin/fidelidade";
    }
    var c = fidelidade.config();
    c.precoPasse = precoPasse; c.diasInatividade = diasInatividade; c.pontosPorReal = pontosPorReal;
    c.pontosMissao = pontosMissao; c.pontosEvento = pontosEvento;
    c.bonusPrimeiroPasse = bonusPrimeiroPasse; c.bonusPasseTemporada = bonusPasseTemporada;
    configuracoes.save(c);
    flash.addFlashAttribute("msg", "Configuração salva. Pontos já concedidos e preços de pedidos existentes foram preservados.");
    return "redirect:/admin/fidelidade";
  }

  @PostMapping("/nivel")
  @Transactional
  public String nivel(@RequestParam String nome, @RequestParam long pontos, @RequestParam String premio,
      @RequestParam(required=false) Long produtoId, RedirectAttributes flash) {
    if (nome.isBlank() || nome.length() > 80 || premio.isBlank() || premio.length() > 120
        || pontos < 1 || pontos > 100000000) {
      flash.addFlashAttribute("erro", "Confira o nome, os pontos e o prêmio.");
      return "redirect:/admin/fidelidade";
    }
    var n = new VipNivel(); n.nome = nome.trim(); n.pontos = pontos; n.premio = premio.trim();
    n.produto = produtoId == null ? null : produtos.findById(produtoId).orElseThrow();
    if (n.produto != null && n.produto.getTipo() != Produto.Tipo.FISICO) throw new IllegalArgumentException("Selecione um produto físico.");
    niveis.save(n);
    return "redirect:/admin/fidelidade";
  }

  @PostMapping("/nivel/{id}/estado")
  @Transactional
  public String estado(@PathVariable Long id) {
    var n = niveis.findById(id).orElseThrow(); n.ativo = !n.ativo;
    return "redirect:/admin/fidelidade";
  }

  @PostMapping("/opcao")
  @Transactional
  public String opcao(@RequestParam Long recompensaId, @RequestParam Long produtoId, RedirectAttributes flash) {
    var recompensa = recompensas.findById(recompensaId).orElseThrow();
    var produto = produtos.findById(produtoId).orElseThrow();
    var existentes = opcoes.findByRecompensaIdOrderByIdAsc(recompensaId);
    if (produto.getTipo() != Produto.Tipo.FISICO || existentes.size() >= 2
        || existentes.stream().anyMatch(o -> o.produto.getId().equals(produtoId))) {
      flash.addFlashAttribute("erro", "Selecione até dois produtos físicos diferentes por recompensa.");
      return "redirect:/admin/fidelidade";
    }
    var opcao = new RecompensaOpcao(); opcao.recompensa = recompensa; opcao.produto = produto;
    opcoes.save(opcao);
    recompensa.setTipoDigital("Fisico");
    return "redirect:/admin/fidelidade";
  }

  @PostMapping("/missao")
  @Transactional
  public String missao(@RequestParam String titulo, @RequestParam PasseAcaoTipo acao,
      @RequestParam PassePeriodicidade periodicidade, @RequestParam int meta, @RequestParam int xp,
      @RequestParam(defaultValue="false") boolean premium, RedirectAttributes flash) {
    if (titulo.isBlank() || titulo.length() > 120 || meta < 1 || meta > 100 || xp < 1 || xp > 3000
        || (premium && acao != PasseAcaoTipo.CHECKIN && acao != PasseAcaoTipo.VIEW_PRODUCT
            && acao != PasseAcaoTipo.ADD_TO_CART)) {
      flash.addFlashAttribute("erro", "Missões Premium devem usar ações simples, sem compra ou avaliação obrigatória.");
      return "redirect:/admin/fidelidade";
    }
    var mes = YearMonth.now();
    var m = new PasseMissaoTemplate();
    m.setTemporada(temporadas.findByAnoAndMes(mes.getYear(), mes.getMonthValue()).orElseThrow());
    m.setCodigo("admin-" + java.util.UUID.randomUUID());
    m.setTitulo(titulo); m.setDescricao(titulo); m.setAcaoTipo(acao); m.setPeriodicidade(periodicidade);
    m.setMetaValor(meta); m.setXpRecompensa(xp); m.setPremium(premium); m.setOrdemExibicao(100);
    missoes.save(m);
    return "redirect:/admin/fidelidade";
  }
}
