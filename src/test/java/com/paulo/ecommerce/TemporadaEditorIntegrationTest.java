package com.paulo.ecommerce;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import com.paulo.ecommerce.service.*;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class TemporadaEditorIntegrationTest {
 @Autowired TemporadaEditorService editor; @Autowired PasseTemporadaRepository temporadas;
 @Autowired PasseRecompensaNivelRepository premios; @Autowired ProdutoRepository produtos;
 @Autowired RecompensaOpcaoRepository opcoes; @Autowired FidelidadeService fidelidade;
 @Autowired PasseService passes; @Autowired MockMvc mvc;
 @Autowired UsuarioRepository usuarios; @Autowired PedidoRepository pedidos; @Autowired BauItemRepository bau;
 private PasseTemporada draft(){fidelidade.config();var mes=YearMonth.now().plusMonths(2);return editor.criar("Operação Neon",mes.atDay(1),mes.atEndOfMonth(),new BigDecimal("79.00"),100,3,5);}
 private void preparar(PasseTemporada t){editor.exemplos(t.getId());editor.missao(t.getId(),"Presença Premium",PasseAcaoTipo.CHECKIN,PassePeriodicidade.DIARIA,1,300,true);}
 private Produto produto(String nome){var p=new Produto();p.setNome(nome);p.setPreco(new BigDecimal("100"));p.setEstoque(100);return produtos.saveAndFlush(p);}
 @Test void seasonPriceIsUsedByCheckoutAndDraftCannotBeBought(){
  var mes=YearMonth.now();var t=temporadas.findByAnoAndMes(mes.getYear(),mes.getMonthValue()).orElseThrow();
  t.setGerenciada(true);t.setPreco(new BigDecimal("89.00"));
  var u=new Usuario();u.setNome("Cliente editor");u.setEmail("editor-season@example.test");u.setSenha("hashed");usuarios.saveAndFlush(u);
  var id=passes.criarPedidoPremium(u.getEmail());var pedido=pedidos.findById(id).orElseThrow();
  assertEquals(0,pedido.getTotal().compareTo(new BigDecimal("89.00")));assertEquals(0,pedido.getItens().get(0).getPrecoUnitario().compareTo(new BigDecimal("89.00")));
  fidelidade.config().precoPasse=new BigDecimal("99.00");assertEquals(0,pedido.getTotal().compareTo(new BigDecimal("89.00")));
  t.setEstado("RASCUNHO");t.setAtiva(false);assertThrows(IllegalStateException.class,()->passes.criarPedidoPremium(u.getEmail()));
  assertThrows(IllegalStateException.class,()->passes.validarTemporadaCompra(t.getId()));
 }
 @Test void draftsArePrivateAndPublishedRulesCannotBeEdited()throws Exception{
  var t=draft();preparar(t);assertFalse(t.disponivel());assertEquals("RASCUNHO",t.getEstado());
  mvc.perform(get("/admin/temporadas/"+t.getId()).with(user("cliente").roles("CLIENTE"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/temporadas/"+t.getId()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Prévia das trilhas e custos")));
  editor.publicar(t.getId(),"admin@example.test");assertEquals("PUBLICADO",t.getEstado());assertFalse(t.disponivel());assertEquals("admin@example.test",t.getPublicadoPor());assertNotNull(t.getPublicadoEm());
  assertThrows(IllegalStateException.class,()->editor.premio(t.getId(),30,true,"Novo prêmio","Alterado","Badge"));
  assertThrows(IllegalStateException.class,()->editor.encerrar(t.getId()));
 }
 @Test void worstChoiceCostsIncludeBothTracksAndAggregateSkuStock(){
  var t=draft();preparar(t);var a=produto("Camisa");var b=produto("Mochila");
  editor.premio(t.getId(),30,true,"Escolha seu prêmio","Camisa ou mochila","Fisico");editor.premio(t.getId(),30,false,"Prêmio gratuito","Camisa","Fisico");
  var rs=premios.findByTemporadaIdOrderByNivelAsc(t.getId());var premium=rs.stream().filter(r->r.getNivel()==30&&r.isPremium()).findFirst().orElseThrow();var free=rs.stream().filter(r->r.getNivel()==30&&!r.isPremium()).findFirst().orElseThrow();
  editor.opcao(t.getId(),premium.getId(),a.getId(),new BigDecimal("20"),new BigDecimal("5"));editor.opcao(t.getId(),premium.getId(),b.getId(),new BigDecimal("30"),new BigDecimal("10"));editor.opcao(t.getId(),free.getId(),a.getId(),new BigDecimal("20"),new BigDecimal("5"));
  var p=editor.previa(t.getId());assertEquals(0,p.custo().compareTo(new BigDecimal("400")));assertEquals(0,p.receita().compareTo(new BigDecimal("395")));assertEquals(0,p.saldo().compareTo(new BigDecimal("-5")));
  a.setEstoque(12);produtos.flush();assertTrue(editor.previa(t.getId()).pendencias().stream().anyMatch(e->e.contains("Estoque insuficiente")));
  assertThrows(IllegalStateException.class,()->editor.publicar(t.getId(),"admin"));
  a.setEstoque(13);editor.publicar(t.getId(),"admin");assertEquals(13,a.getEstoque());
 }
 @Test void cannotPublishIncompleteSeasonOrSpendMissionInPremium(){
  var t=draft();assertFalse(editor.previa(t.getId()).pendencias().isEmpty());assertThrows(IllegalStateException.class,()->editor.publicar(t.getId(),"admin"));
  assertThrows(IllegalArgumentException.class,()->editor.missao(t.getId(),"Comprar",PasseAcaoTipo.PURCHASE_APPROVED,PassePeriodicidade.MENSAL,1,300,true));
  assertThrows(IllegalArgumentException.class,()->editor.criar("Duplicada",t.getInicioEm(),t.getFimEm(),new BigDecimal("79"),100,1,1));
  assertThrows(IllegalArgumentException.class,()->editor.criar("Período inválido",t.getInicioEm(),t.getFimEm().plusDays(1),new BigDecimal("79"),100,1,1));
 }
 @Test void rewardOptionsCannotCrossSeasonsOrExceedTwo(){
  var t=draft();preparar(t);editor.premio(t.getId(),30,true,"Final","Escolha um produto","Fisico");var r=premios.findByTemporadaIdOrderByNivelAsc(t.getId()).stream().filter(x->x.getNivel()==30&&x.isPremium()).findFirst().orElseThrow();
  var a=produto("A");editor.opcao(t.getId(),r.getId(),a.getId(),BigDecimal.ONE,BigDecimal.ZERO);editor.opcao(t.getId(),r.getId(),a.getId(),BigDecimal.TEN,BigDecimal.ZERO);assertEquals(1,opcoes.findByRecompensaIdOrderByIdAsc(r.getId()).size());
  editor.opcao(t.getId(),r.getId(),produto("B").getId(),BigDecimal.ONE,BigDecimal.ZERO);assertThrows(IllegalArgumentException.class,()->editor.opcao(t.getId(),r.getId(),produto("C").getId(),BigDecimal.ONE,BigDecimal.ZERO));
  var o=opcoes.findByRecompensaIdOrderByIdAsc(r.getId()).get(0);editor.removerOpcao(t.getId(),o.id);assertEquals(1,opcoes.findByRecompensaIdOrderByIdAsc(r.getId()).size());
 }
 @Test void publishedOverlapIsRejectedAndEndedSeasonKeepsItsRewards(){
  var t=draft();preparar(t);var outra=new PasseTemporada();outra.setNome("Conflito");outra.setSlug("conflito-editor");outra.setAno(2099);outra.setMes(1);outra.setTotalNiveis(30);outra.setXpPorNivel(100);outra.setInicioEm(t.getInicioEm());outra.setFimEm(t.getFimEm());temporadas.saveAndFlush(outra);
  assertThrows(IllegalStateException.class,()->editor.publicar(t.getId(),"admin"));outra.setEstado("ENCERRADO");editor.publicar(t.getId(),"admin");
  var item=new BauItem();item.usuarioId=987L;item.origem="PASSE-TESTE-"+t.getId();item.titulo="Camisa já resgatada";bau.saveAndFlush(item);
  t.setFimEm(LocalDate.now().minusDays(1));int antes=premios.findByTemporadaIdOrderByNivelAsc(t.getId()).size();editor.encerrar(t.getId());assertEquals("ENCERRADO",t.getEstado());assertEquals(antes,premios.findByTemporadaIdOrderByNivelAsc(t.getId()).size());assertTrue(bau.existsById(item.id));
 }
}
