package com.paulo.ecommerce;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import com.paulo.ecommerce.service.PasseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class CommerceIntegrationTest {
 @Autowired MockMvc mvc; @Autowired UsuarioRepository users; @Autowired ProdutoRepository products;
 @Autowired PedidoRepository orders; @Autowired PasseService passes; @Autowired PasseProgressoUsuarioRepository progress;
 private Usuario account(){var u=new Usuario();u.setNome("Cliente teste");u.setEmail("checkout@example.test");u.setSenha("hashed-test-password");return users.saveAndFlush(u);}
 private Produto product(){var p=new Produto();p.setNome("Produto teste");p.setPreco(new BigDecimal("100.00"));p.setEstoque(5);return products.saveAndFlush(p);}
 @Test void checkoutIsIdempotentAndUsesDatabasePrices() throws Exception {
  var u=account();var p=product();String body="{\"idempotencyKey\":\"unique-checkout-12345\",\"nome\":\"Cliente teste\",\"telefone\":\"11999999999\",\"cep\":\"01001000\",\"endereco\":\"Rua teste\",\"numero\":\"10\",\"bairro\":\"Centro\",\"cidade\":\"São Paulo\",\"estado\":\"SP\",\"pagamento\":\"pix\",\"itens\":[{\"produtoId\":"+p.getId()+",\"quantidade\":2}]}";
  for(int i=0;i<2;i++)mvc.perform(post("/api/checkout/orders").with(user(u.getEmail()).roles("CLIENTE")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
  assertEquals(1,orders.findByUsuarioIdOrderByDataPedidoDesc(u.getId()).size());
  assertEquals(3,products.findById(p.getId()).orElseThrow().getEstoque());
  assertEquals(0,new BigDecimal("214.90").compareTo(orders.findByUsuarioIdOrderByDataPedidoDesc(u.getId()).get(0).getTotal()));
 }
 @Test void browserCannotClaimPurchasesOrAmounts() throws Exception {
  var u=account();mvc.perform(post("/api/pass/actions").with(user(u.getEmail()).roles("CLIENTE")).with(csrf()).contentType("application/json").content("{\"acao\":\"PURCHASE_APPROVED\",\"valor\":100}" )).andExpect(status().isBadRequest());
  assertEquals(0,passes.carregarDashboard(u.getEmail()).xpTotal());
 }
 @Test void viewsRequireDistinctProductsAndPremiumIsGated() throws Exception {
  var u=account();var p=product();
  for(int i=0;i<3;i++)passes.registrarAcaoNavegador(u.getEmail(),PasseAcaoTipo.VIEW_PRODUCT,"product-"+p.getId());
  var dashboard=passes.carregarDashboard(u.getEmail());assertEquals(0,dashboard.xpTotal());
  assertEquals(1,dashboard.missoesDiarias().stream().filter(m -> m.titulo().equals("Ver 3 produtos")).findFirst().orElseThrow().progressoAtual());
  var premium=dashboard.niveis().stream().filter(n -> n.premium()).findFirst().orElseThrow();
  mvc.perform(post("/api/pass/rewards/"+premium.recompensaId()+"/claim").with(user(u.getEmail()).roles("CLIENTE")).with(csrf())).andExpect(status().isBadRequest());
 }
 @Test void couponBelongsToAccountAndCannotBeUsedTwice() throws Exception {
  var u=account();var season=passes.carregarDashboard(u.getEmail());
  var state=progress.findByUsuarioIdAndTemporadaId(u.getId(),progress.findAll().get(0).getTemporada().getId()).orElseThrow();state.setXpTotal(100);progress.saveAndFlush(state);
  var reward=season.niveis().stream().filter(n->n.tipoDigital().equals("Cupom")).findFirst().orElseThrow();
  var claim=passes.reclamarRecompensa(u.getEmail(),reward.recompensaId());
  assertTrue(claim.codigo().startsWith("LC-"));
  assertTrue(passes.colecao(u.getEmail()).stream().anyMatch(r->r.get("codigo").equals(claim.codigo())));
  var order=new Pedido();order.setUsuario(u);order.setNumero("COUPON-TEST");order.setTotal(new BigDecimal("100"));order.setSubtotal(new BigDecimal("100"));order=orders.saveAndFlush(order);
  assertEquals(0,new BigDecimal("10").compareTo(passes.usarCupom(u,claim.codigo(),order)));
  final Pedido otherOrder=order;assertThrows(IllegalArgumentException.class,()->passes.usarCupom(u,claim.codigo(),otherOrder));
 }
}
