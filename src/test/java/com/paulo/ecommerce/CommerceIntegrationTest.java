package com.paulo.ecommerce;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import com.paulo.ecommerce.service.PasseService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommerceIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UsuarioRepository users;
  @Autowired ProdutoRepository products;
  @Autowired PedidoRepository orders;
  @Autowired com.paulo.ecommerce.service.PedidoService orderService;
  @Autowired com.paulo.ecommerce.service.EstoqueService estoque;
  @Autowired PasseService passes;
  @Autowired PasseProgressoUsuarioRepository progress;

  private Usuario account() {
    var u = new Usuario();
    u.setNome("Cliente teste");
    u.setEmail("checkout@example.test");
    u.setSenha("hashed-test-password");
    return users.saveAndFlush(u);
  }

  private Produto product() {
    var p = new Produto();
    p.setNome("Produto teste");
    p.setPreco(new BigDecimal("100.00"));
    p.setEstoque(5);
    return products.saveAndFlush(p);
  }

  @Test
  void registrationCannotReuseAnExistingAccountIdOrSetItsOwnRole() throws Exception {
    var existing = account();
    mvc.perform(post("/auth/cadastro")
            .with(csrf())
            .param("id", existing.getId().toString())
            .param("role", "ADMIN")
            .param("temaPasse", "azul")
            .param("nome", "Outra conta")
            .param("email", "nova-conta@example.test")
            .param("senha", "uma-senha-de-teste"))
        .andExpect(redirectedUrl("/login?sucesso=true"));

    var created = users.findByEmail("nova-conta@example.test").orElseThrow();
    assertNotEquals(existing.getId(), created.getId());
    assertEquals(Usuario.Role.CLIENTE, created.getRole());
    assertNull(created.getTemaPasse());
    assertEquals("checkout@example.test", users.findById(existing.getId()).orElseThrow().getEmail());
  }

  @Test
  void checkoutIsIdempotentAndUsesDatabasePrices() throws Exception {
    var u = account();
    var p = product();
    String body =
        "{\"idempotencyKey\":\"unique-checkout-12345\",\"nome\":\"Cliente"
            + " teste\",\"telefone\":\"11999999999\",\"cep\":\"01001000\",\"endereco\":\"Rua"
            + " teste\",\"numero\":\"10\",\"bairro\":\"Centro\",\"cidade\":\"São"
            + " Paulo\",\"estado\":\"SP\",\"pagamento\":\"pix\",\"itens\":[{\"produtoId\":"
            + p.getId()
            + ",\"quantidade\":2}]}";
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/api/checkout/orders")
                  .with(user(u.getEmail()).roles("CLIENTE"))
                  .with(csrf())
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isOk());
    assertEquals(1, orders.findByUsuarioIdOrderByDataPedidoDesc(u.getId()).size());
    assertEquals(3, products.findById(p.getId()).orElseThrow().getEstoque());
    assertEquals(
        0,
        new BigDecimal("214.90")
            .compareTo(orders.findByUsuarioIdOrderByDataPedidoDesc(u.getId()).get(0).getTotal()));
  }

  @Test
  void browserCannotClaimPurchasesOrAmounts() throws Exception {
    var u = account();
    mvc.perform(
            post("/api/pass/actions")
                .with(user(u.getEmail()).roles("CLIENTE"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"acao\":\"PURCHASE_APPROVED\",\"valor\":100}"))
        .andExpect(status().isBadRequest());
    assertEquals(0, passes.carregarDashboard(u.getEmail()).xpTotal());
  }

  @Test
  void viewsRequireDistinctProductsAndPremiumIsGated() throws Exception {
    var u = account();
    var p = product();
    for (int i = 0; i < 3; i++)
      passes.registrarAcaoNavegador(
          u.getEmail(), PasseAcaoTipo.VIEW_PRODUCT, "product-" + p.getId());
    var dashboard = passes.carregarDashboard(u.getEmail());
    assertEquals(0, dashboard.xpTotal());
    assertEquals(
        1,
        dashboard.missoesDiarias().stream()
            .filter(m -> m.titulo().equals("Ver 3 produtos"))
            .findFirst()
            .orElseThrow()
            .progressoAtual());
    var premium = dashboard.niveis().stream().filter(n -> n.premium()).findFirst().orElseThrow();
    mvc.perform(
            post("/api/pass/rewards/" + premium.recompensaId() + "/claim")
                .with(user(u.getEmail()).roles("CLIENTE"))
                .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void couponBelongsToAccountAndCannotBeUsedTwice() throws Exception {
    var u = account();
    var season = passes.carregarDashboard(u.getEmail());
    var state =
        progress
            .findByUsuarioIdAndTemporadaId(
                u.getId(), progress.findAll().get(0).getTemporada().getId())
            .orElseThrow();
    state.setXpTotal(100);
    progress.saveAndFlush(state);
    var reward =
        season.niveis().stream()
            .filter(n -> n.tipoDigital().equals("Cupom"))
            .findFirst()
            .orElseThrow();
    var claim = passes.reclamarRecompensa(u.getEmail(), reward.recompensaId());
    assertTrue(claim.codigo().startsWith("LC-"));
    assertTrue(
        passes.colecao(u.getEmail()).stream()
            .anyMatch(r -> r.get("codigo").equals(claim.codigo())));
    var produto = product();
    var payload = """
        {"itens":[{"produtoId":%d,"quantidade":1}],"cupom":"%s"}
        """.formatted(produto.getId(), claim.codigo());
    mvc.perform(post("/api/checkout/quote").with(csrf())
            .contentType("application/json").content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.desconto").value(0))
        .andExpect(jsonPath("$.cupomErro").value("Entre na sua conta para aplicar um cupom."));
    mvc.perform(post("/api/checkout/quote").with(user(u.getEmail()).roles("CLIENTE")).with(csrf())
            .contentType("application/json").content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subtotal").value(100))
        .andExpect(jsonPath("$.frete").value(24.9))
        .andExpect(jsonPath("$.desconto").value(10))
        .andExpect(jsonPath("$.total").value(114.9));
    assertEquals(5, products.findById(produto.getId()).orElseThrow().getEstoque());

    var order = new Pedido();
    order.setUsuario(u);
    order.setNumero("COUPON-TEST");
    order.setTotal(new BigDecimal("100"));
    order.setSubtotal(new BigDecimal("100"));
    order = orders.saveAndFlush(order);
    assertEquals(0, new BigDecimal("10").compareTo(passes.usarCupom(u, claim.codigo(), order)));
    var otherOrder = new Pedido();
    otherOrder.setUsuario(u);
    otherOrder.setNumero("COUPON-SECOND-ORDER");
    otherOrder.setSubtotal(new BigDecimal("100"));
    otherOrder.setTotal(new BigDecimal("100"));
    orders.saveAndFlush(otherOrder);
    assertThrows(
        IllegalArgumentException.class, () -> passes.usarCupom(u, claim.codigo(), otherOrder));

    order.setStatus(Pedido.Status.CANCELADO);
    orders.saveAndFlush(order);
    assertEquals(0, new BigDecimal("10").compareTo(passes.usarCupom(u, claim.codigo(), otherOrder)));
  }

  @Test
  void purchaseXpIsIdempotentAndRefundReversesPurchaseMissions() {
    var u = account();
    var p = product();
    var order = new Pedido();
    order.setUsuario(u);
    order.setNumero("REFUND-TEST");
    order.setSubtotal(new BigDecimal("500"));
    order.setTotal(new BigDecimal("500"));
    order.setStatus(Pedido.Status.CONFIRMADO);
    var item = new ItemPedido();
    item.setPedido(order);
    item.setProduto(p);
    item.setQuantidade(5);
    item.setPrecoUnitario(new BigDecimal("100"));
    order.setItens(java.util.List.of(item));
    var payment = new Pagamento();
    payment.setPedido(order);
    payment.setStatus(Pagamento.StatusPagamento.APROVADO);
    payment.setPagoEm(java.time.LocalDateTime.now());
    order.setPagamento(payment);
    order = orders.saveAndFlush(order);
    passes.registrarXpCompraAprovada(u.getEmail(), order.getId(), order.getSubtotal());
    int xp = passes.carregarDashboard(u.getEmail()).xpTotal();
    assertTrue(xp > 0);
    passes.registrarXpCompraAprovada(u.getEmail(), order.getId(), order.getSubtotal());
    assertEquals(xp, passes.carregarDashboard(u.getEmail()).xpTotal());
    payment.setStatus(Pagamento.StatusPagamento.REEMBOLSADO);
    order.setStatus(Pedido.Status.CANCELADO);
    orders.saveAndFlush(order);
    passes.estornarCompra(u.getEmail(), order.getId());
    assertEquals(0, passes.carregarDashboard(u.getEmail()).xpTotal());
  }

  @Test
  void expiredReservationReturnsStockOnlyOnce() {
    var u = account();
    var p = product();

    var order = new Pedido();
    order.setUsuario(u);
    order.setNumero("EXPIRED-TEST");
    order.setSubtotal(new BigDecimal("200"));
    order.setTotal(new BigDecimal("200"));
    order.setDataPedido(java.time.LocalDateTime.now().minusHours(25));
    var item = new ItemPedido();
    item.setPedido(order);
    item.setProduto(p);
    item.setQuantidade(2);
    item.setPrecoUnitario(new BigDecimal("100"));
    order.setItens(java.util.List.of(item));
    order = orders.saveAndFlush(order);
    estoque.reservar(p,2,"PEDIDO:"+order.getId());
    orderService.expirarReserva(order.getId());
    orderService.expirarReserva(order.getId());
    assertEquals(5, products.findById(p.getId()).orElseThrow().getEstoque());
    assertEquals(Pedido.Status.CANCELADO, orders.findById(order.getId()).orElseThrow().getStatus());
  }

  @Test
  void adminCannotMoveShippedOrderBackToPending() throws Exception {
    var order = new Pedido();
    order.setUsuario(account());
    order.setNumero("SHIPPED-TEST");
    order.setTotal(new BigDecimal("100"));
    order.setStatus(Pedido.Status.ENVIADO);
    order = orders.saveAndFlush(order);

    mvc.perform(
            post("/admin/pedidos/{id}/status", order.getId())
                .with(user("admin@example.test").roles("ADMIN"))
                .with(csrf())
                .param("status", "PENDENTE"))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attributeExists("erro"));
    assertEquals(Pedido.Status.ENVIADO, orders.findById(order.getId()).orElseThrow().getStatus());
  }

  @Test
  void finalRewardRequiresPremiumAndLevelWithoutMinimumSpend() {
    var u = account();
    var dashboard = passes.carregarDashboard(u.getEmail());
    var state =
        progress.findAll().stream()
            .filter(p -> p.getUsuario().getId().equals(u.getId()))
            .findFirst()
            .orElseThrow();
    state.setXpTotal(dashboard.totalNiveis() * dashboard.xpPorNivel());
    progress.saveAndFlush(state);
    var reward =
        dashboard.niveis().stream()
            .filter(n -> n.nivel() == dashboard.totalNiveis() && n.premium())
            .findFirst()
            .orElseThrow();
    assertThrows(
        IllegalStateException.class,
        () -> passes.reclamarRecompensa(u.getEmail(), reward.recompensaId()));
    var order = orders.findById(passes.criarPedidoPremium(u.getEmail())).orElseThrow();
    assertEquals(0, new BigDecimal("79.00").compareTo(order.getTotal()));
    assertTrue(order.isPasseMensal());
    var payment = new Pagamento();
    payment.setPedido(order);
    payment.setStatus(Pagamento.StatusPagamento.APROVADO);
    payment.setPagoEm(java.time.LocalDateTime.now());
    order.setPagamento(payment);
    order.setStatus(Pedido.Status.CONFIRMADO);
    orders.saveAndFlush(order);
    assertTrue(passes.carregarDashboard(u.getEmail()).premiumAtivo());
    assertNotNull(passes.reclamarRecompensa(u.getEmail(), reward.recompensaId()).codigo());
  }

  @Test
  void anotherAccountCannotReadOrStartPaymentForAnOrder() throws Exception {
    var u = account();
    var order = new Pedido();
    order.setUsuario(u);
    order.setNumero("OWNERSHIP-TEST");
    order.setTotal(new BigDecimal("100"));
    order = orders.saveAndFlush(order);
    var other = new Usuario();
    other.setNome("Outro cliente");
    other.setEmail("other@example.test");
    other.setSenha("other-test-password");
    users.saveAndFlush(other);
    mvc.perform(
            get("/api/account/orders/" + order.getId())
                .with(user(other.getEmail()).roles("CLIENTE")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/account/orders/" + order.getId() + "/payment")
                .with(user(other.getEmail()).roles("CLIENTE"))
                .with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/pagamento/iniciar/" + order.getId())
                .with(user(other.getEmail()).roles("CLIENTE"))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }
}
