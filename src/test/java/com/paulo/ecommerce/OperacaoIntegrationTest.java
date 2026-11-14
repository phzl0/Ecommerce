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
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OperacaoIntegrationTest {
 @Autowired ExpedicaoService expedicao;
 @Autowired ProdutoRepository produtos;
 @Autowired UsuarioRepository usuarios;
 @Autowired PagamentoRepository pagamentos;
 @Autowired PedidoService pedidos;
 @Autowired DigitalService digitais;
 @Autowired DigitalEntregaRepository entregas;
 @Autowired DigitalCodigoRepository codigos;
 @Autowired EstoqueService estoque;
 @Autowired EstoqueReservaRepository reservas;
 @Autowired PlatformTransactionManager transactions;
 @Autowired MockMvc mvc;
 private Usuario cliente(String email) {var u=new Usuario();u.setNome("Cliente digital");u.setEmail(email);u.setSenha("hashed-test");return usuarios.saveAndFlush(u);}
 private Produto produto(Produto.Tipo tipo,int saldo) {var p=new Produto();p.setNome("Produto "+UUID.randomUUID());p.setTipo(tipo);p.setPreco(BigDecimal.TEN);p.setEstoque(saldo);return produtos.saveAndFlush(p);}
 private CheckoutForm endereco() {var f=new CheckoutForm();f.setNomeDestinatario("Cliente");f.setTelefoneContato("11999999999");f.setCep("06400000");f.setLogradouro("Rua Teste");f.setNumero("10");f.setBairro("Centro");f.setCidade("Barueri");f.setUf("SP");return f;}
 private Pedido comprar(Usuario u,Produto... ps) {return pedidos.finalizarPedidoApi(u.getEmail(),endereco(),Arrays.stream(ps).map(p->new CheckoutItemRequest(p.getId(),1)).toList(),UUID.randomUUID().toString());}
 private void aprovar(Pedido p) {var pg=new Pagamento();pg.setPedido(p);pg.setStatus(Pagamento.StatusPagamento.APROVADO);pg.setValor(p.getTotal());pg.setMetodo(Pagamento.MetodoPagamento.PIX);pg.setPagoEm(java.time.LocalDateTime.now());pagamentos.saveAndFlush(pg);p.setPagamento(pg);pedidos.atualizarStatus(p.getId(),Pedido.Status.CONFIRMADO);}
 @Test void mixedOrderReleasesDigitalOnlyAfterApprovalAndKeepsPhysicalReserved() throws Exception {
  var u=cliente("mixed@example.test");var file=produto(Produto.Tipo.DIGITAL,2);var physical=produto(Produto.Tipo.FISICO,2);
  digitais.arquivo(file.getId(),"demo.txt","version one".getBytes());var p=comprar(u,file,physical);
  var e=entregas.findByPedidoIdOrderByIdAsc(p.getId()).get(0);
  assertEquals("PENDENTE",e.estado);assertTrue(p.getFrete().signum()>0);
  mvc.perform(get("/api/account/digitais/"+e.id+"/arquivo").with(user(u.getEmail()).roles("CLIENTE"))).andExpect(status().isForbidden());
  aprovar(p);digitais.liberar(p);assertEquals(1,entregas.findByPedidoIdOrderByIdAsc(p.getId()).size());
  assertEquals(0,reservas.totalReservado(file.getId(),"PEDIDO:%"));assertEquals(1,reservas.totalReservado(physical.getId(),"PEDIDO:%"));
  digitais.arquivo(file.getId(),"demo.txt","version two".getBytes());
  assertEquals("version one",new String(digitais.baixar(u.getEmail(),e.id).dados));
  cliente("other-digital@example.test");
  mvc.perform(get("/api/account/digitais/"+e.id+"/arquivo").with(user("other-digital@example.test").roles("CLIENTE"))).andExpect(status().isForbidden());
  mvc.perform(get("/api/account/digitais/"+e.id+"/arquivo").with(user(u.getEmail()).roles("CLIENTE")))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
  expedicao.iniciar(p.getId());
  var quantidades=new java.util.HashMap<Long,Integer>();
  for(var item:p.getItens())if(item.getTipo()==Produto.Tipo.FISICO)quantidades.put(item.getId(),item.getQuantidade());
  expedicao.conferir(p.getId(),quantidades,"teste");
  expedicao.enviar(p.getId(),"Transportadora demo","DEMO-123","https://example.com/rastreio");
  assertEquals(0,reservas.totalReservado(physical.getId(),"PEDIDO:%"));assertEquals(1,physical.getEstoque());
 }
 @Test void cancelledPendingCodeIsReleasedButRefundedApprovedCodeIsNeverRecycled() {
  var u=cliente("codes@example.test");var p=produto(Produto.Tipo.DIGITAL,0);
  assertEquals(2,digitais.importarCodigos(p.getId(),"DEMO-A\nDEMO-A\nDEMO-B","teste"));
  var first=comprar(u,p);pedidos.cancelarPedido(first.getId(),u.getEmail());assertEquals(2,p.getEstoque());
  var second=comprar(u,p);aprovar(second);var e=entregas.findByPedidoIdOrderByIdAsc(second.getId()).get(0);
  assertEquals("DEMO-A",digitais.revelar(u.getEmail(),e.id));
  second.getPagamento().setStatus(Pagamento.StatusPagamento.REEMBOLSADO);pedidos.atualizarStatus(second.getId(),Pedido.Status.CANCELADO);
  assertEquals("REVOGADO",e.estado);assertEquals(1,p.getEstoque());assertEquals(1,codigos.countByProdutoIdAndEstado(p.getId(),"INUTILIZADO"));
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->digitais.revelar(u.getEmail(),e.id));
 }
 @Test void customerCannotAdjustStockAndExpiryRestoresOnlyOnce() throws Exception {
  var u=cliente("expiry-digital@example.test");var p=produto(Produto.Tipo.FISICO,5);var order=comprar(u,p);
  estoque.ajustar(p.getId(),2,3,"Recebimento do fornecedor","teste");assertEquals(6,p.getEstoque());
  mvc.perform(post("/admin/estoque/"+p.getId()+"/ajustar").with(user(u.getEmail()).roles("CLIENTE")).with(csrf())
   .param("quantidade","100").param("minimo","3").param("motivo","Fraude teste")).andExpect(status().isForbidden());
  order.setDataPedido(java.time.LocalDateTime.now().minusHours(25));pedidos.expirarReserva(order.getId());pedidos.expirarReserva(order.getId());
  assertEquals(7,p.getEstoque());assertEquals(0,reservas.totalReservado(p.getId(),"PEDIDO:%"));
 }
 @Test
 @Transactional(propagation=Propagation.NOT_SUPPORTED)
 void concurrentReservationsCannotSellLastUnitTwice() throws Exception {
  var tx=new TransactionTemplate(transactions);var p=tx.execute(s->produto(Produto.Tipo.FISICO,1));
  var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try {
   List<Future<Boolean>> results=new ArrayList<>();
   for(int i=0;i<2;i++) {final int n=i;results.add(pool.submit(()->{start.await();try {estoque.reservar(p,1,"CONCURRENCY:"+n);return true;}catch(IllegalStateException e){return false;}}));}
   start.countDown();int successes=0;for(var r:results) if(r.get(15,TimeUnit.SECONDS)) successes++;
   assertEquals(1,successes);assertEquals(0,produtos.findById(p.getId()).orElseThrow().getEstoque());
  } finally {pool.shutdownNow();}
 }
}
