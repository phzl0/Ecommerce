package com.paulo.ecommerce;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.paulo.ecommerce.model.*;import com.paulo.ecommerce.repository.*;import com.paulo.ecommerce.service.*;
import java.math.BigDecimal;import java.time.*;import java.util.*;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.test.context.ActiveProfiles;import org.springframework.test.web.servlet.MockMvc;import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(properties={"app.local-demo-enabled=true","app.payments.enabled=false"}) @AutoConfigureMockMvc @ActiveProfiles("local") @Transactional
class MissaoGestaoIntegrationTest {
 @Autowired MissaoGestaoService gestao;@Autowired TemporadaEditorService editor;@Autowired PasseService passe;@Autowired PasseTemporadaRepository temporadas;@Autowired PasseMissaoTemplateRepository missoes;@Autowired ProdutoRepository produtos;@Autowired CategoriaRepository categorias;@Autowired UsuarioRepository usuarios;@Autowired PasseMissaoProgressoRepository progresso;@Autowired PasseMissaoAcaoRepository acoes;@Autowired PagamentoService pagamentos;@Autowired MockMvc mvc;@Autowired EventoSemanalRepository eventos;
 private PasseTemporada draft(){var mes=YearMonth.now().plusMonths(3);return editor.criar("Missões teste",mes.atDay(1),mes.atEndOfMonth(),new BigDecimal("79"),100,1,1);}
 private void salvar(PasseTemporada t,Long id,String nome,int xp,boolean premium,PassePeriodicidade periodo,int meta,Long produto){gestao.salvar(t.getId(),id,nome,"Descrição da missão",produto==null?PasseAcaoTipo.CHECKIN:PasseAcaoTipo.VIEW_PRODUCT,periodo,meta,xp,premium,false,10,produto,null,null,t.getInicioEm(),t.getFimEm());}
 private PasseMissaoTemplate primeira(PasseTemporada t){return missoes.findByTemporadaIdOrderByOrdemExibicaoAscIdAsc(t.getId()).get(0);}
 private Produto produto(){var p=new Produto();p.setNome("Produto missão");p.setPreco(BigDecimal.TEN);p.setEstoque(5);return produtos.saveAndFlush(p);}
 private Usuario usuario(){var u=new Usuario();u.setNome("Cliente missão");u.setEmail("mission-management@example.test");u.setSenha("hashed-test");return usuarios.saveAndFlush(u);}
 @Test void editsDuplicatesAndOrderAreLimitedToDraftAndOwnSeason()throws Exception{
  var t=draft();salvar(t,null,"Original",100,false,PassePeriodicidade.DIARIA,1,null);var m=primeira(t);salvar(t,m.getId(),"Editada",250,true,PassePeriodicidade.DIARIA,1,null);assertEquals(250,m.getXpRecompensa());gestao.duplicar(t.getId(),m.getId());assertEquals(2,missoes.findByTemporadaIdOrderByOrdemExibicaoAscIdAsc(t.getId()).size());gestao.ordem(t.getId(),m.getId(),99);assertEquals(99,m.getOrdemExibicao());
  mvc.perform(get("/admin/temporadas/"+t.getId()+"/missoes").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());mvc.perform(get("/admin/temporadas/"+t.getId()+"/missoes").with(user("client").roles("CLIENTE"))).andExpect(status().isForbidden());
  assertThrows(IllegalArgumentException.class,()->gestao.buscar(987L,m.getId()));t.setEstado("PUBLICADO");assertThrows(IllegalStateException.class,()->gestao.ordem(t.getId(),m.getId(),1));assertEquals(250,m.getXpRecompensa());
 }
 @Test void simulatorCombinesFreeAndPremiumAndWarnsForLateEntry(){
  var t=draft();salvar(t,null,"Gratuita",100,false,PassePeriodicidade.DIARIA,1,null);salvar(t,null,"Premium",200,true,PassePeriodicidade.DIARIA,1,null);
  var s=gestao.simular(t.getId(),t.getInicioEm());assertEquals(10,s.dias());assertEquals(t.getInicioEm().plusDays(9),s.conclusao());assertNull(gestao.simular(t.getId(),t.getFimEm()).conclusao());assertThrows(IllegalArgumentException.class,()->gestao.simular(t.getId(),t.getFimEm().plusDays(1)));
 }
 @Test void simulatorRespectsWeeklyMetaAndCatalogCapacity(){
  var t=draft();salvar(t,null,"Dois checkins",3000,true,PassePeriodicidade.SEMANAL,2,null);var s=gestao.simular(t.getId(),t.getInicioEm());assertTrue(s.dias()>=2);var p=produto();salvar(t,null,"Ver opção",3000,true,PassePeriodicidade.SEMANAL,2,p.getId());
  assertThrows(IllegalArgumentException.class,()->salvar(t,null,"Meta impossível",100,true,PassePeriodicidade.DIARIA,2,p.getId()));
 }
 @Test void productScopedProgressCountsReferencesOnceAndIgnoresOtherSku(){
  var mes=YearMonth.now();var t=temporadas.findByAnoAndMes(mes.getYear(),mes.getMonthValue()).orElseThrow();t.setGerenciada(true);var p=produto();var outro=produto();
  var m=new PasseMissaoTemplate();m.setTemporada(t);m.setCodigo("scope-test");m.setTitulo("Veja o produto");m.setDescricao("Descrição");m.setAcaoTipo(PasseAcaoTipo.VIEW_PRODUCT);m.setPeriodicidade(PassePeriodicidade.SEMANAL);m.setMetaValor(2);m.setXpRecompensa(100);m.setProdutoId(p.getId());missoes.saveAndFlush(m);var u=usuario();
  passe.registrarAcao(u.getEmail(),PasseAcaoTipo.VIEW_PRODUCT,1,"produto-"+outro.getId());passe.registrarAcao(u.getEmail(),PasseAcaoTipo.VIEW_PRODUCT,1,"produto-"+p.getId());passe.registrarAcao(u.getEmail(),PasseAcaoTipo.VIEW_PRODUCT,1,"produto-"+p.getId());
  var estado=progresso.findByUsuarioIdAndTemporadaId(u.getId(),t.getId()).stream().filter(x->x.getTemplate().getId().equals(m.getId())).findFirst().orElseThrow();assertEquals(1,estado.getProgressoAtual());assertFalse(estado.getConcluida());
  var lembretes=passe.missoesDoProduto(u.getEmail(),p.getId());assertTrue(lembretes.stream().anyMatch(x->x.id().equals(m.getId())&&x.ctaHref().equals("/produto/"+p.getId())));assertTrue(passe.missoesDoProduto(u.getEmail(),outro.getId()).stream().noneMatch(x->x.id().equals(m.getId())));
 }
 @Test void categoriesEventsAndDatesGateActionsAndPremiumCannotRequireReview(){
  var t=draft();var c=new Categoria();c.setNome("Categoria missão");categorias.saveAndFlush(c);var p=produto();p.setCategoria(c);produtos.flush();
  gestao.salvar(t.getId(),null,"Categoria","Descrição",PasseAcaoTipo.VIEW_PRODUCT,PassePeriodicidade.MENSAL,1,100,false,true,1,null,c.getId(),null,t.getInicioEm(),t.getFimEm());var m=primeira(t);assertTrue(m.isEspecial());assertFalse(gestao.aceita(m,"produto-"+p.getId()));m.setInicioEm(LocalDate.now());m.setFimEm(LocalDate.now());t.setInicioEm(LocalDate.now());t.setFimEm(LocalDate.now());assertTrue(gestao.aceita(m,"produto-"+p.getId()));
  var e=new EventoSemanal();e.titulo="Evento";e.descricao="Evento";e.selo="Selo";e.inicio=LocalDateTime.now().plusDays(1);e.fim=e.inicio.plusDays(1);e.minimoCompra=BigDecimal.ZERO;e.multiplicador=1;e.limiteXp=100;e.metaXp=1;eventos.saveAndFlush(e);m.setEventoId(e.id);assertFalse(gestao.aceita(m,"produto-"+p.getId()));
  assertThrows(IllegalArgumentException.class,()->gestao.salvar(t.getId(),null,"Review","Descrição",PasseAcaoTipo.REVIEW_SUBMITTED,PassePeriodicidade.MENSAL,1,100,true,false,1,null,null,null,t.getInicioEm(),t.getFimEm()));
 }
 @Test void lockedPremiumMissionDoesNotProgressBeforePurchase(){
  var mes=YearMonth.now();var t=temporadas.findByAnoAndMes(mes.getYear(),mes.getMonthValue()).orElseThrow();t.setGerenciada(true);var m=new PasseMissaoTemplate();m.setTemporada(t);m.setCodigo("premium-once");m.setTitulo("Premium fácil");m.setDescricao("Checkin");m.setAcaoTipo(PasseAcaoTipo.CHECKIN);m.setPeriodicidade(PassePeriodicidade.MENSAL);m.setMetaValor(1);m.setXpRecompensa(250);m.setPremium(true);m.setEspecial(true);missoes.saveAndFlush(m);var u=usuario();
  passe.registrarAcao(u.getEmail(),PasseAcaoTipo.CHECKIN,1,"checkin");assertTrue(progresso.findByUsuarioIdAndTemporadaId(u.getId(),t.getId()).stream().noneMatch(x->x.getTemplate().getId().equals(m.getId())));
  pagamentos.simularAprovacao(passe.criarPedidoPremium(u.getEmail()));passe.registrarAcao(u.getEmail(),PasseAcaoTipo.CHECKIN,1,"checkin");passe.registrarAcao(u.getEmail(),PasseAcaoTipo.CHECKIN,1,"checkin");
  var estados=progresso.findByUsuarioIdAndTemporadaId(u.getId(),t.getId()).stream().filter(x->x.getTemplate().getId().equals(m.getId())).toList();assertEquals(1,estados.size());assertTrue(estados.get(0).getConcluida());assertTrue(passe.carregarDashboard(u.getEmail()).missoesMensais().stream().anyMatch(x->x.id().equals(m.getId())&&x.especial()));
 }
}
