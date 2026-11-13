package com.paulo.ecommerce;

import static org.junit.jupiter.api.Assertions.*;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import com.paulo.ecommerce.service.*;
import com.paulo.ecommerce.dto.CheckoutForm;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FidelidadeIntegrationTest {
 @Autowired ExpedicaoService expedicao;
  @Autowired UsuarioRepository usuarios;
  @Autowired ProdutoRepository produtos;
  @Autowired PedidoRepository pedidos;
  @Autowired BauService bau;
  @Autowired BauItemRepository itens;
  @Autowired PedidoService operacao;
  @Autowired FidelidadeService vip;
  @Autowired VipContaRepository contas;
  @Autowired VipNivelRepository niveis;
  @Autowired PasseService passe;
  @Autowired PasseProgressoUsuarioRepository progressos;
  @Autowired PasseTemporadaRepository temporadas;

  private Usuario cliente() {
    var u = new Usuario(); u.setNome("Cliente"); u.setEmail("fidelidade@example.test"); u.setSenha("hashed-test");
    return usuarios.saveAndFlush(u);
  }

  @Test
  void rewardReservesStockOnceAndUsesNormalShippingWithIdempotentDelivery() {
    var u = cliente();
    var p = new Produto(); p.setNome("Camisa M"); p.setPreco(new BigDecimal("49")); p.setEstoque(2);
    produtos.saveAndFlush(p);
    var premio = bau.conceder(u.getId(), "PASSE-TESTE", "Camisa M", p);
    assertEquals(premio.id, bau.conceder(u.getId(), "PASSE-TESTE", "Camisa M", p).id);
    assertEquals(1, p.getEstoque());
    var form = endereco();
    Long id = bau.solicitar(u.getEmail(), premio.id, form);
    assertEquals(id, bau.solicitar(u.getEmail(), premio.id, form));
    var pedido = pedidos.findById(id).orElseThrow();
    assertTrue(pedido.isRecompensa());
    assertEquals(Pedido.Status.CONFIRMADO, pedido.getStatus());
    assertEquals(0, pedido.getTotal().signum());
    assertEquals(0, pedido.getFrete().signum());
    assertNull(pedido.getPagamento());
    assertTrue(operacao.acoesOperacionais(pedido).contains(Pedido.Status.ENVIADO));
    expedicao.iniciar(id);
    var quantidades = new java.util.HashMap<Long,Integer>();
    for(var item : pedido.getItens()) quantidades.put(item.getId(),item.getQuantidade());
    expedicao.conferir(id,quantidades,"teste");
    expedicao.enviar(id,"Transportadora demo","DEMO-123","https://example.com/rastreio");
    operacao.atualizarStatusOperacional(id, Pedido.Status.ENTREGUE);
    assertEquals(Pedido.Status.ENTREGUE, pedido.getStatus());
    assertEquals(1, p.getEstoque());
  }

  @Test
  void cancelledRewardKeepsReservationAndCanBeRequestedAgain() {
    var u = cliente();
    var p = new Produto(); p.setNome("Camisa"); p.setPreco(BigDecimal.TEN); p.setEstoque(1);
    produtos.saveAndFlush(p);
    var premio = bau.conceder(u.getId(), "VIP-TESTE", "Camisa", p);
    Long primeiro = bau.solicitar(u.getEmail(), premio.id, endereco());
    operacao.atualizarStatusOperacional(primeiro, Pedido.Status.CANCELADO);
    Long segundo = bau.solicitar(u.getEmail(), premio.id, endereco());
    assertNotEquals(primeiro, segundo);
    assertEquals(0, p.getEstoque());
  }

  @Test
  void vipExpiresButClaimedRewardsSurviveAndCannotBeClaimedTwice() {
    var u = cliente();
    var n = new VipNivel(); n.nome = "Bronze"; n.pontos = 10; n.premio = "Selo Bronze";
    niveis.saveAndFlush(n);
    vip.creditar(u.getId(), "EVENTO-TESTE", 20, null);
    vip.creditar(u.getId(), "EVENTO-TESTE", 20, null);
    assertEquals(20L, vip.perfil(u.getEmail()).get("pontos"));
    Long premio = vip.resgatar(u.getEmail(), n.id);
    contas.findById(u.getId()).orElseThrow().ultimaAtividade = LocalDateTime.now().minusDays(91);
    assertEquals(0L, vip.perfil(u.getEmail()).get("pontos"));
    assertEquals(1, bau.listar(u.getEmail()).size());
    vip.creditar(u.getId(), "EVENTO-NOVO", 30, null);
    assertEquals(premio, vip.resgatar(u.getEmail(), n.id));
    assertEquals(1, bau.listar(u.getEmail()).size());
  }

  @Test
  void premiumIsSeasonBoundAndBothFinalTracksUseSameProgress() {
    var u = cliente();
    var dashboard = passe.carregarDashboard(u.getEmail());
    assertEquals(2, dashboard.niveis().stream().filter(n -> n.nivel() == dashboard.totalNiveis()).count());
    assertTrue(dashboard.missoesDiarias().stream().anyMatch(m -> m.premium()));
    passe.registrarAcaoNavegador(u.getEmail(), PasseAcaoTipo.CHECKIN, null);
    assertTrue(passe.carregarDashboard(u.getEmail()).missoesDiarias().stream()
        .filter(m -> m.premium()).allMatch(m -> m.progressoAtual() == 0));
    var pedido = pedidos.findById(passe.criarPedidoPremium(u.getEmail())).orElseThrow();
    var pg = new Pagamento(); pg.setPedido(pedido); pg.setStatus(Pagamento.StatusPagamento.APROVADO);
    pg.setPagoEm(LocalDateTime.now()); pedido.setPagamento(pg); pedido.setStatus(Pedido.Status.CONFIRMADO);
    pedidos.saveAndFlush(pedido);
    var ativo = passe.carregarDashboard(u.getEmail());
    assertTrue(ativo.premiumAtivo());
    assertEquals(YearMonth.now().atEndOfMonth().plusDays(1).atStartOfDay(), ativo.premiumValidoAte());
    var progresso = progressos.findAll().stream().filter(p -> p.getUsuario().getId().equals(u.getId())).findFirst().orElseThrow();
    progresso.setXpTotal(ativo.totalNiveis() * ativo.xpPorNivel());
    for (var premio : ativo.niveis().stream().filter(n -> n.nivel() == ativo.totalNiveis()).toList())
      assertNotNull(passe.reclamarRecompensa(u.getEmail(), premio.recompensaId()).codigo());
    pedido.setTemporadaPasseId(-1L);
    assertFalse(passe.carregarDashboard(u.getEmail()).premiumAtivo());
  }

  @Test
  void digitalOnlyCartHasNoShipping() {
    var u = cliente();
    var p = new Produto(); p.setNome("Arquivo demo"); p.setPreco(BigDecimal.TEN); p.setEstoque(5); p.setTipo(Produto.Tipo.DIGITAL);
    produtos.saveAndFlush(p);
    var cotacao = operacao.cotar(u.getEmail(), null,
        java.util.List.of(new com.paulo.ecommerce.dto.api.CheckoutItemRequest(p.getId(), 1)));
    assertEquals(0, cotacao.frete().signum());
  }

  private CheckoutForm endereco() {
    var f = new CheckoutForm(); f.setNomeDestinatario("Cliente"); f.setTelefoneContato("11999999999");
    f.setCep("06400-000"); f.setLogradouro("Rua de teste"); f.setNumero("10"); f.setBairro("Centro");
    f.setCidade("Barueri"); f.setUf("SP");
    return f;
  }
}
