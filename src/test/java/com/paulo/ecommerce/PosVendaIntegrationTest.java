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
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class PosVendaIntegrationTest {
 @Autowired ProdutoRepository produtos;
 @Autowired UsuarioRepository usuarios;
 @Autowired PedidoService pedidos;
 @Autowired PagamentoRepository pagamentos;
 @Autowired ExpedicaoService envio;
 @Autowired PosVendaService atendimento;
 @Autowired PedidoExpedicaoRepository expedicoes;
 @Autowired PedidoDevolucaoRepository devolucoes;
 @Autowired PedidoMovimentacaoRepository movimentos;
 @Autowired MockMvc mvc;
 private Pedido pedido() {
  var u=new Usuario();u.setNome("Cliente pós-venda");u.setEmail("posvenda@example.test");u.setSenha("hashed-test");usuarios.saveAndFlush(u);
  var p=new Produto();p.setNome("Produto de teste");p.setPreco(BigDecimal.TEN);p.setEstoque(5);produtos.saveAndFlush(p);
  var f=new CheckoutForm();f.setNomeDestinatario(u.getNome());f.setTelefoneContato("11999999999");f.setCep("06400000");f.setLogradouro("Rua Teste");f.setNumero("10");f.setBairro("Centro");f.setCidade("Barueri");f.setUf("SP");
  var o=pedidos.finalizarPedidoApi(u.getEmail(),f,List.of(new CheckoutItemRequest(p.getId(),2)),UUID.randomUUID().toString());
  var pg=new Pagamento();pg.setPedido(o);pg.setStatus(Pagamento.StatusPagamento.APROVADO);pg.setValor(o.getTotal());pg.setMetodo(Pagamento.MetodoPagamento.PIX);pagamentos.saveAndFlush(pg);o.setPagamento(pg);pedidos.atualizarStatus(o.getId(),Pedido.Status.CONFIRMADO);return o;
 }
 private void despachar(Pedido p) {
  envio.iniciar(p.getId());envio.conferir(p.getId(),Map.of(p.getItens().get(0).getId(),2),"operador");
  envio.enviar(p.getId(),"Transportadora demo","DEMO-123","https://example.com/rastreio");
 }
 @Test void shipmentRequiresExactConferenceAndRepeatedDispatchDoesNotDuplicateEvents() {
  var p=pedido();envio.iniciar(p.getId());
  assertThrows(IllegalArgumentException.class,()->envio.conferir(p.getId(),Map.of(p.getItens().get(0).getId(),1),"operador"));
  assertThrows(IllegalStateException.class,()->pedidos.atualizarStatusOperacional(p.getId(),Pedido.Status.ENVIADO));
  assertEquals(Pedido.Status.CONFIRMADO,p.getStatus());despachar(p);
  envio.enviar(p.getId(),"Transportadora demo","DEMO-123","https://example.com/rastreio");
  assertEquals(1,movimentos.findByPedidoIdOrderByIdDesc(p.getId()).stream().filter(m->"Pedido enviado".equals(m.getDescricao())).count());
  envio.entregar(p.getId());envio.entregar(p.getId());assertEquals("ENTREGUE",expedicoes.findById(p.getId()).orElseThrow().etapa);
 }
 @Test void returnsAreBoundedAndOnlyInspectedUnitsAreRestockedOnce() {
  var p=pedido();despachar(p);var item=p.getItens().get(0);String email=p.getUsuario().getEmail();
  var id=atendimento.solicitar(p.getId(),email,item.getId(),2,"DEVOLUCAO","Produto não corresponde ao esperado","return-test-001");
  assertEquals(id,atendimento.solicitar(p.getId(),email,item.getId(),2,"DEVOLUCAO","Produto não corresponde ao esperado","return-test-001"));
  assertEquals(3,item.getProduto().getEstoque());
  atendimento.atualizar(p.getId(),id,"autorizar",0,"Retorno autorizado; siga as instruções.","operador");
  atendimento.atualizar(p.getId(),id,"receber",1,"Recebemos duas unidades; uma apta para revenda.","operador");
  atendimento.atualizar(p.getId(),id,"receber",1,"Recebemos duas unidades; uma apta para revenda.","operador");
  assertEquals(4,item.getProduto().getEstoque());assertEquals(1,devolucoes.findById(id).orElseThrow().reposta);
  atendimento.atualizar(p.getId(),id,"concluir",0,"Solução registrada para acompanhamento financeiro.","operador");
  assertEquals(Pagamento.StatusPagamento.APROVADO,p.getPagamento().getStatus());
  assertThrows(IllegalArgumentException.class,()->atendimento.solicitar(p.getId(),email,item.getId(),1,"TROCA","Solicitação além do total permitido","return-test-002"));
 }
 @Test void privateNotesNeverAppearInCustomerApiAndOtherCustomersCannotAccess() throws Exception {
  var p=pedido();var email=p.getUsuario().getEmail();
  atendimento.mensagemAdmin(p.getId(),"admin@example.test","NOTA INTERNA SIGILOSA","admin-note-001",false);
  atendimento.mensagemCliente(p.getId(),email,"Gostaria de uma atualização","client-note-001");
  atendimento.mensagemCliente(p.getId(),email,"Gostaria de uma atualização","client-note-001");
  atendimento.mensagemAdmin(p.getId(),"admin@example.test","Seu pedido está em preparação.","admin-note-002",true);
  var response=mvc.perform(get("/api/account/orders/"+p.getId()+"/operation").with(user(email).roles("CLIENTE"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertFalse(response.contains("NOTA INTERNA SIGILOSA"));assertFalse(response.contains("admin@example.test"));assertTrue(response.contains("Seu pedido"));
  mvc.perform(get("/api/account/orders/"+p.getId()+"/operation").with(user("intruso@example.test").roles("CLIENTE"))).andExpect(status().isForbidden());
  mvc.perform(post("/admin/pedidos/"+p.getId()+"/separar").with(user(email).roles("CLIENTE")).with(csrf())).andExpect(status().isForbidden());
 }
 @Test void trackingLinksRejectExecutableSchemesAndCredentials() {
  for(var url:List.of("javascript:alert(1)","http://example.com","https://user:pass@example.com/x","https://localhost/x","https://127.0.0.1/x"))
   assertThrows(IllegalArgumentException.class,()->ExpedicaoService.urlSegura(url));
  assertEquals("https://example.com/rastreio?q=123",ExpedicaoService.urlSegura("https://example.com/rastreio?q=123"));
 }
}
