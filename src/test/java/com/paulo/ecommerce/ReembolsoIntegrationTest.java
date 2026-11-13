package com.paulo.ecommerce;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import com.paulo.ecommerce.service.*;
import com.paulo.ecommerce.dto.CheckoutForm;
import com.paulo.ecommerce.dto.api.CheckoutItemRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(properties={"app.local-demo-enabled=true","app.payments.enabled=false"})
@ActiveProfiles("local") @AutoConfigureMockMvc @Transactional
class ReembolsoIntegrationTest {
 @Autowired PedidoService pedidos; @Autowired PedidoRepository repo;
 @Autowired UsuarioRepository usuarios; @Autowired ProdutoRepository produtos;
 @Autowired PagamentoService pagamentos; @Autowired PasseService passe;
 @Autowired ReembolsoService reembolso; @Autowired PedidoReembolsoRepository refunds;
 @Autowired ImpactoReembolsoService impactos; @Autowired FidelidadeService vip;
 @Autowired PasseXpHistoricoRepository xp; @Autowired VipMovimentoRepository movimentos;
 @Autowired EventoSemanalRepository eventosRepo; @Autowired EventoSemanalService eventos;
 @Autowired PosVendaService pos; @Autowired TrocaService trocas; @Autowired ExpedicaoService envio;
 @Autowired MockMvc mvc;
 private Pedido compra(){
  var u=new Usuario();u.setNome("Cliente reembolso");u.setEmail("refund@example.test");u.setSenha("hashed-test");usuarios.saveAndFlush(u);
  var p=new Produto();p.setNome("Camisa teste");p.setPreco(new BigDecimal("100"));p.setEstoque(10);produtos.saveAndFlush(p);
  var f=new CheckoutForm();f.setNomeDestinatario(u.getNome());f.setTelefoneContato("11999999999");f.setCep("06400000");f.setLogradouro("Rua Teste");f.setNumero("10");f.setBairro("Centro");f.setCidade("Barueri");f.setUf("SP");
  var o=pedidos.finalizarPedidoApi(u.getEmail(),f,List.of(new CheckoutItemRequest(p.getId(),2)),UUID.randomUUID().toString());pagamentos.simularAprovacao(o.getId());return o;
 }
 private Long solicitar(Pedido p,BigDecimal valor){var a=impactos.prever(p,valor);return reembolso.solicitar(p.getId(),valor,"Devolução solicitada pelo cliente",UUID.randomUUID().toString(),a.token(),true,"admin@example.test");}
 private void confirmar(Pedido p,Long id){var r=refunds.findById(id).orElseThrow();var a=impactos.prever(p,r.valor);reembolso.confirmarSimulado(p.getId(),id,a.token(),true,"admin@example.test");}
 @Test void partialRefundsUseCumulativeRoundingAndNeverDuplicateReversal(){
  var p=compra();var metade=p.getTotal().divide(BigDecimal.valueOf(2));
  long antes=(long)vip.perfil(p.getUsuario().getEmail()).get("pontos");
  var aviso=impactos.prever(p,metade);var id=solicitar(p,metade);
  assertEquals(0,p.getValorReembolsado().signum());assertEquals(antes,vip.perfil(p.getUsuario().getEmail()).get("pontos"));
  confirmar(p,id);assertEquals(antes-aviso.vip(),vip.perfil(p.getUsuario().getEmail()).get("pontos"));
  assertEquals(Pagamento.StatusPagamento.APROVADO,p.getPagamento().getStatus());
  var h=xp.findByUsuarioIdAndOrigemTipoAndReferencia(p.getUsuario().getId(),PasseOrigemXpTipo.PURCHASE,"PURCHASE-"+p.getId()).get(0);
  assertEquals(h.getXpGanho()/2,h.getXpEstornado());
  reembolso.confirmarSimulado(p.getId(),id,aviso.token(),true,"admin@example.test");assertEquals(0,metade.compareTo(p.getValorReembolsado()));
  confirmar(p,solicitar(p,metade));assertTrue(h.isEstornado());assertEquals(Pagamento.StatusPagamento.REEMBOLSADO,p.getPagamento().getStatus());assertEquals(Pedido.Status.CANCELADO,p.getStatus());
  assertEquals(10,p.getItens().get(0).getProduto().getEstoque());
 }
 @Test void consentStalePreviewAndPendingAmountsAreEnforced(){
  var p=compra();var valor=BigDecimal.TEN;var a=impactos.prever(p,valor);
  assertThrows(IllegalArgumentException.class,()->reembolso.solicitar(p.getId(),valor,"Motivo válido","test-key-001",a.token(),false,"admin"));
  var first=solicitar(p,valor);var second=solicitar(p,valor);confirmar(p,first);
  assertThrows(IllegalStateException.class,()->reembolso.confirmarSimulado(p.getId(),second,a.token(),true,"admin"));
  assertThrows(IllegalStateException.class,()->solicitar(p,p.getTotal().subtract(valor)));
  reembolso.cancelar(p.getId(),second,"admin");assertEquals("CANCELADO",refunds.findById(second).orElseThrow().estado);
 }
 @Test void eventThresholdAndClaimedVipBonusAreIncludedInPreview(){
  var e=new EventoSemanal();e.titulo="Evento teste";e.descricao="Evento";e.inicio=LocalDateTime.now().minusHours(1);e.fim=LocalDateTime.now().plusDays(1);e.multiplicador=2;e.limiteXp=100;e.metaXp=30;e.minimoCompra=new BigDecimal("150");e.selo="Selo teste";eventosRepo.saveAndFlush(e);
  var p=compra();eventos.resgatar(e.id,p.getUsuario().getEmail());
  var metade=p.getTotal().divide(BigDecimal.valueOf(2));var a=impactos.prever(p,metade);
  assertEquals(40,a.eventos().get(0).antes());assertEquals(0,a.eventos().get(0).depois());assertTrue(a.analisarBeneficios());
  confirmar(p,solicitar(p,metade));assertTrue(movimentos.findByUsuarioIdAndReferencia(p.getUsuario().getId(),"EVENTO-"+e.id).orElseThrow().estornado);
  assertTrue(eventos.selos(p.getUsuario().getId()).isEmpty());assertTrue(refunds.findByPedidoIdOrderByIdDesc(p.getId()).get(0).analiseBeneficios);
 }
 @Test void realProviderPaymentCannotBeRefundedBySimulation(){
  var p=compra();p.getPagamento().setMpPaymentId("real-payment-id");var id=solicitar(p,BigDecimal.TEN);
  assertThrows(IllegalStateException.class,()->confirmar(p,id));assertEquals(0,p.getValorReembolsado().signum());
 }
 @Test void replacementReservesOnceUsesShippingAndDoesNotAwardPoints(){
  var p=compra();envio.iniciar(p.getId());envio.conferir(p.getId(),Map.of(p.getItens().get(0).getId(),2),"admin");envio.enviar(p.getId(),"Demo","TESTE",null);
  var d=pos.solicitar(p.getId(),p.getUsuario().getEmail(),p.getItens().get(0).getId(),1,"TROCA","Troca de tamanho solicitada","exchange-key-001");
  pos.atualizar(p.getId(),d,"autorizar",0,"Retorno autorizado","admin");pos.atualizar(p.getId(),d,"receber",1,"Recebido e conferido","admin");
  var produto=p.getItens().get(0).getProduto();int saldo=produto.getEstoque();long pontos=(long)vip.perfil(p.getUsuario().getEmail()).get("pontos");
  var novoId=trocas.criar(p.getId(),d,produto.getId(),"Troca de tamanho",true,"admin");assertEquals(novoId,trocas.criar(p.getId(),d,produto.getId(),"Troca de tamanho",true,"admin"));
  var novo=repo.findById(novoId).orElseThrow();assertEquals(p.getId(),novo.getPedidoOrigemId());assertEquals(0,novo.getTotal().signum());assertEquals(saldo-1,produto.getEstoque());assertEquals(pontos,vip.perfil(p.getUsuario().getEmail()).get("pontos"));
  envio.iniciar(novoId);envio.conferir(novoId,Map.of(novo.getItens().get(0).getId(),1),"admin");envio.enviar(novoId,"Demo","TROCA-1",null);assertEquals(Pedido.Status.ENVIADO,novo.getStatus());
  assertThrows(IllegalStateException.class,()->solicitar(p,BigDecimal.TEN));
 }
 @Test void impactEndpointAndRefundOperationsArePrivate() throws Exception {
  var p=compra();var path="/api/account/orders/"+p.getId()+"/operation/impact?itemId="+p.getItens().get(0).getId()+"&quantidade=1";
  mvc.perform(get(path).with(user("intruso@example.test").roles("CLIENTE"))).andExpect(status().isForbidden());
  mvc.perform(get(path).with(user(p.getUsuario().getEmail()).roles("CLIENTE"))).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
  mvc.perform(post("/admin/pedidos/"+p.getId()+"/reembolso").with(user(p.getUsuario().getEmail()).roles("CLIENTE")).with(csrf())).andExpect(status().isForbidden());
 }
}
