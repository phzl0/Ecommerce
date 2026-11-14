package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FidelidadeDemoService {
  @org.springframework.beans.factory.annotation.Autowired private DigitalService digitais;
  @org.springframework.beans.factory.annotation.Autowired private DigitalConfigRepository digitalConfigs;
  private final ProdutoRepository produtos;
  private final CategoriaRepository categorias;
  private final VipNivelRepository niveis;
  private final PasseTemporadaRepository temporadas;
  private final PasseRecompensaNivelRepository recompensas;
  private final RecompensaOpcaoRepository opcoes;
  public FidelidadeDemoService(ProdutoRepository produtos, CategoriaRepository categorias,
      VipNivelRepository niveis, PasseTemporadaRepository temporadas,
      PasseRecompensaNivelRepository recompensas, RecompensaOpcaoRepository opcoes) {
    this.produtos = produtos; this.categorias = categorias; this.niveis = niveis;
    this.temporadas = temporadas; this.recompensas = recompensas; this.opcoes = opcoes;
  }

  @Transactional
  public void preparar() {
    var camisa = produto("DEMO-CAMISA-M", "Camisa LEVEL · tamanho M · demonstração", Produto.Tipo.FISICO, "Moda LEVEL");
    var mochila = produto("DEMO-MOCHILA", "Mochila LEVEL · demonstração", Produto.Tipo.FISICO, "Acessórios LEVEL");
    var pack = produto("DEMO-WALLPAPER", "Pack digital LEVEL · demonstração", Produto.Tipo.DIGITAL, "Digitais LEVEL");
    if(!digitalConfigs.existsById(pack.getId())) digitais.arquivo(pack.getId(),"leia-me-level-demo.txt",
        "LEVEL Commerce — arquivo demonstrativo. Nenhum conteúdo comercial incluído.".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var licenca=produto("DEMO-LICENCA", "Código LEVEL · demonstração", Produto.Tipo.DIGITAL,"Digitais LEVEL");
    if(!digitalConfigs.existsById(licenca.getId())) {
      licenca.setEstoque(0);
      digitais.importarCodigos(licenca.getId(),"DEMO-SEM-VALOR-001\nDEMO-SEM-VALOR-002\nDEMO-SEM-VALOR-003", "demo");
    }
    if (produtos.findBySku("DEMO-CATALOGO-P").isEmpty()) {
      var principal = produto("DEMO-CATALOGO-P", "Camiseta LEVEL Essential · demo", Produto.Tipo.FISICO, "Moda LEVEL");
      principal.setTamanho("P"); principal.setCor("Preto"); principal.setPesoKg(new BigDecimal("0.250"));
      principal.setImagemUrl("/app/level-free.webp"); principal.setGaleria("/app/level-premium.webp");
      principal.setXpExtra(50); principal.setXpExtraInicio(java.time.LocalDateTime.now().minusDays(1)); principal.setXpExtraFim(java.time.LocalDateTime.now().plusDays(30));
      produtos.saveAndFlush(principal);
      var variacao = produto("DEMO-CATALOGO-M", principal.getNome(), Produto.Tipo.FISICO, "Moda LEVEL");
      variacao.setProdutoPrincipalId(principal.getId()); variacao.setTamanho("M"); variacao.setCor("Verde"); variacao.setPreco(new BigDecimal("59.90"));
      variacao.setImagemUrl("/app/level-premium.webp"); produtos.save(variacao);
    }
    if (niveis.count() == 0) {
      nivel("Bronze", 100, "Selo Bronze LEVEL", null);
      nivel("Prata", 500, "Selo Prata LEVEL", null);
      nivel("Ouro", 1500, "Camisa LEVEL · demonstração", camisa);
    }
    var mes = YearMonth.now();
    var temporada = temporadas.findByAnoAndMes(mes.getYear(), mes.getMonthValue()).orElseThrow();
    recompensas.findByTemporadaIdOrderByNivelAsc(temporada.getId()).stream()
        .filter(r -> r.isPremium() && r.getNivel().equals(temporada.getTotalNiveis()))
        .filter(r -> opcoes.findByRecompensaIdOrderByIdAsc(r.getId()).isEmpty())
        .forEach(r -> {
          r.setTitulo("Escolha seu prêmio LEVEL");
          r.setDescricao("Camisa ou mochila de demonstração. Resgate para guardar no Baú.");
          r.setTipoDigital("Fisico");
          for (var produto : java.util.List.of(camisa, mochila)) {
            var opcao = new RecompensaOpcao(); opcao.recompensa = r; opcao.produto = produto;
            opcoes.save(opcao);
          }
        });
  }

  private Produto produto(String sku, String nome, Produto.Tipo tipo, String categoria) {
    return produtos.findBySku(sku).orElseGet(() -> {
      var c = categorias.findByNomeIgnoreCase(categoria).orElseGet(() -> {
        var nova = new Categoria(); nova.setNome(categoria); nova.setDescricao("Catálogo demonstrativo.");
        return categorias.save(nova);
      });
      var p = new Produto(); p.setSku(sku); p.setNome(nome); p.setTipo(tipo); p.setCategoria(c);
      p.setDescricaoCurta("Produto demonstrativo para testar a operação LEVEL.");
      p.setDescricao("Exemplo de desenvolvimento. Configure fornecedor, conteúdo e condições antes de vender.");
      p.setPreco(new BigDecimal("49.90")); p.setEstoque(100);
      p.setImagemUrl("/app/level-free.webp");
      return produtos.save(p);
    });
  }

  private void nivel(String nome, long pontos, String premio, Produto produto) {
    var n = new VipNivel(); n.nome = nome; n.pontos = pontos; n.premio = premio; n.produto = produto;
    niveis.save(n);
  }
}
