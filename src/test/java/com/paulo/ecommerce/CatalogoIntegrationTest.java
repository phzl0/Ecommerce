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
class CatalogoIntegrationTest {
 @Autowired ProdutoService catalogo; @Autowired ProdutoRepository produtos; @Autowired StorefrontService loja;
 @Autowired PedidoService pedidos; @Autowired UsuarioRepository usuarios; @Autowired PagamentoService pagamentos;
 @Autowired PasseXpHistoricoRepository historico; @Autowired ImpactoReembolsoService impactos;
 @Autowired ReembolsoService reembolsos; @Autowired MockMvc mvc;
 private Produto produto(String nome,String tamanho,Long pai){
  var p=new Produto();p.setNome(nome);p.setPreco(new BigDecimal("100.00"));p.setEstoque(5);p.setTamanho(tamanho);p.setCor("Preto");p.setProdutoPrincipalId(pai);
  return catalogo.salvarCadastro(p,null);
 }
 @Test void oneListingGroupsVariantsAndFiltersMatchTheSameSku(){
  var p=produto("Família catálogo","P",null);var m=produto(p.getNome(),"M",p.getId());m.setPreco(new BigDecimal("120.00"));produtos.flush();
  var lista=loja.carregarCatalogo("Família catálogo",null,null,null,"preco-asc","preto","M","FISICO",0);
  assertEquals(1,lista.total());assertTrue(lista.produtos().get(0).temVariacoes());assertEquals(10,lista.produtos().get(0).estoque());
  assertEquals(0,loja.carregarCatalogo("Família catálogo",null,null,new BigDecimal("110"),"preco-asc","preto","M","FISICO",0).total());
  var detalhe=loja.carregarProduto(m.getId());assertEquals(p.getId(),detalhe.familiaId());assertEquals(2,detalhe.variacoes().size());assertEquals("M · Preto",detalhe.opcao());
  assertEquals(0,detalhe.preco().compareTo(new BigDecimal("120")));
 }
 @Test void draftParentBlocksChildPurchaseAndNestedOrDuplicateVariantsAreRejected(){
  var p=produto("Família privada","P",null);var m=produto(p.getNome(),"M",p.getId());
  assertThrows(IllegalArgumentException.class,()->produto(p.getNome(),"m",p.getId()));
  assertThrows(IllegalArgumentException.class,()->produto(p.getNome(),"G",m.getId()));
  catalogo.definirVisibilidade(p.getId(),false);
  assertFalse(catalogo.disponivelNaLoja(m));assertThrows(RuntimeException.class,()->loja.carregarProduto(m.getId()));
  assertThrows(com.paulo.ecommerce.exception.EstoqueInsuficienteException.class,()->pedidos.cotar(null,null,List.of(new CheckoutItemRequest(m.getId(),1))));
 }
 @Test void galleryOrderingAndRemovalAreBoundedAndChildFallsBackToParent(){
  var p=produto("Galeria","P",null);var m=produto(p.getNome(),"M",p.getId());
  catalogo.galeria(p.getId(),List.of("/images/a.png","/images/b.png"),null,"adicionar");
  catalogo.galeria(p.getId(),List.of(),1,"capa");assertEquals("/images/b.png",loja.carregarProduto(m.getId()).imagens().get(0));
  assertThrows(IllegalArgumentException.class,()->catalogo.galeria(p.getId(),Collections.nCopies(7,"/images/c.png"),null,"adicionar"));
  assertThrows(IllegalArgumentException.class,()->catalogo.galeria(p.getId(),List.of(),8,"remover"));
  catalogo.galeria(p.getId(),List.of(),0,"remover");assertEquals(List.of("/images/a.png"),p.imagens());
 }
 @Test void orderKeepsSkuAndBonusAndRefundReversesTheAwardOnce(){
  var p=produto("Nome comprado","M",null);p.setXpExtra(80);p.setXpExtraInicio(LocalDateTime.now().minusHours(1));p.setXpExtraFim(LocalDateTime.now().plusDays(1));produtos.flush();
  assertEquals(160,pedidos.cotar(null,null,List.of(new CheckoutItemRequest(p.getId(),2))).xpExtra());
  var u=new Usuario();u.setNome("Cliente catálogo");u.setEmail("catalogo@example.test");u.setSenha("hashed");usuarios.saveAndFlush(u);
  var f=new CheckoutForm();f.setNomeDestinatario(u.getNome());f.setTelefoneContato("11999999999");f.setCep("06400000");f.setLogradouro("Rua Teste");f.setNumero("10");f.setBairro("Centro");f.setCidade("Barueri");f.setUf("SP");
  var pedido=pedidos.finalizarPedidoApi(u.getEmail(),f,List.of(new CheckoutItemRequest(p.getId(),2)),"catalogo-key-001");
  String sku=p.getSku();p.setNome("Novo nome");p.setTamanho("G");p.setXpExtra(999);produtos.flush();
  assertEquals("Nome comprado · M · Preto",pedido.getItens().get(0).getNomeSnapshot());assertEquals(sku,pedido.getItens().get(0).getSkuSnapshot());
  pagamentos.simularAprovacao(pedido.getId());assertThrows(IllegalStateException.class,()->pagamentos.simularAprovacao(pedido.getId()));
  var xp=historico.findByUsuarioIdAndOrigemTipoAndReferencia(u.getId(),PasseOrigemXpTipo.PURCHASE,"PURCHASE-"+pedido.getId());assertEquals(1,xp.size());assertTrue(xp.get(0).getXpGanho()>=160);
  assertTrue(xp.get(0).getXpGanho()<999);
  var impacto=impactos.prever(pedido,pedido.getTotal());
  var id=reembolsos.solicitar(pedido.getId(),pedido.getTotal(),"Reembolso catálogo","catalogo-refund-key",impacto.token(),true,"admin");
  reembolsos.confirmarSimulado(pedido.getId(),id,impacto.token(),true,"admin");assertEquals(xp.get(0).getXpGanho(),xp.get(0).getXpEstornado());
 }
 @Test void adminShowsNewSectionsAndDoesNotBindGalleryUrls()throws Exception{
  var p=produto("Editor","P",null);
  mvc.perform(get("/admin/produtos/"+p.getId()+"/editar").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Variações e estoque")));
  mvc.perform(post("/admin/produtos/salvar").with(user("admin").roles("ADMIN")).with(csrf()).param("id",p.getId().toString()).param("nome","Editor").param("sku",p.getSku()).param("preco","100.00").param("estoque","5").param("estoqueOriginal","5").param("publicacao","RASCUNHO").param("galeria","https://evil.example/x").param("imagemUrl","https://evil.example/x"))
    .andExpect(status().is3xxRedirection());assertFalse(catalogo.disponivelNaLoja(p));assertTrue(p.imagens().isEmpty());
 }
}
