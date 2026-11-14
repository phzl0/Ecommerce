package com.paulo.ecommerce.service;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class TrocaService {
 @org.springframework.beans.factory.annotation.Autowired private PedidoReembolsoRepository reembolsos;
 private final PedidoService pedidos;
 private final PedidoRepository repo;
 private final PedidoDevolucaoRepository devolucoes;
 private final ProdutoRepository produtos;
 private final EstoqueService estoque;
 private final PedidoMovimentacaoRepository historico;
 private final NotificacaoService notificacoes;
 public TrocaService(PedidoService p,PedidoRepository r,PedidoDevolucaoRepository d,ProdutoRepository pr,EstoqueService e,PedidoMovimentacaoRepository h,NotificacaoService n){pedidos=p;repo=r;devolucoes=d;produtos=pr;estoque=e;historico=h;notificacoes=n;}
 public Long criar(Long id,Long solicitacao,Long produtoId,String motivo,boolean ciente,String autor){
  var origem=pedidos.bloquearPedidoParaConciliacao(id);var d=devolucoes.findById(solicitacao).orElseThrow();
  if(!d.pedidoId.equals(id))throw new IllegalArgumentException("Solicitação não pertence ao pedido.");
  if(d.pedidoTrocaId!=null)return d.pedidoTrocaId;
  if(!ciente||motivo==null||motivo.strip().length()<5||motivo.length()>1000)throw new IllegalArgumentException("Informe o motivo e confirme as condições da troca.");
  if(!"TROCA".equals(d.tipo)||!"RECEBIDA".equals(d.estado))throw new IllegalStateException("A troca exige solicitação recebida e conferida.");
  if(origem.getValorReembolsado().signum()>0||origem.getStatus()==Pedido.Status.CANCELADO)throw new IllegalStateException("Pedido já reembolsado exige análise antes de uma troca.");
  if(origem.getEnderecoEntrega()==null||origem.getEnderecoEntrega().isBlank())throw new IllegalStateException("Confira o endereço do pedido original.");
  if(reembolsos.findByPedidoIdOrderByIdDesc(id).stream().anyMatch(r->"PENDENTE".equals(r.estado)))throw new IllegalStateException("Resolva o reembolso pendente antes de criar a troca.");
  var produto=produtos.findLockedById(produtoId).orElseThrow();
  if(!produto.isAtivo()||produto.getTipo()!=Produto.Tipo.FISICO||(produto.getSku()!=null&&produto.getSku().startsWith("PASSE-")))throw new IllegalArgumentException("Escolha um produto físico ativo.");
  var novo=new Pedido();novo.setPedidoOrigemId(id);novo.setUsuario(origem.getUsuario());novo.setNumero("TR-"+UUID.randomUUID().toString().replace("-","").substring(0,24));
  novo.setStatus(Pedido.Status.CONFIRMADO);novo.setTotal(BigDecimal.ZERO);novo.setSubtotal(BigDecimal.ZERO);
  novo.setNomeDestinatario(origem.getNomeDestinatario());novo.setTelefoneContato(origem.getTelefoneContato());novo.setEnderecoEntrega(origem.getEnderecoEntrega());
  novo.setCepEntrega(origem.getCepEntrega());novo.setCidadeEntrega(origem.getCidadeEntrega());novo.setEstadoEntrega(origem.getEstadoEntrega());novo.setComplementoEntrega(origem.getComplementoEntrega());
  novo.setObservacoes("Troca da solicitação #"+d.id+" · "+motivo.strip());
  var item=new ItemPedido();item.setPedido(novo);item.setProduto(produto);item.setTipo(Produto.Tipo.FISICO);item.setQuantidade(d.quantidade);item.setPrecoUnitario(BigDecimal.ZERO);novo.setItens(List.of(item));
  repo.saveAndFlush(novo);estoque.reservar(produto,d.quantidade,"PEDIDO:"+novo.getId());d.pedidoTrocaId=novo.getId();
  historico.save(new PedidoMovimentacao(id,origem.getStatus(),"Troca vinculada ao pedido "+novo.getNumero()+" · "+autor));
  historico.save(new PedidoMovimentacao(novo.getId(),novo.getStatus(),"Troca do pedido "+origem.getNumero()+" · sem cobrança e sem novos pontos · "+autor));
  notificacoes.registrar(origem.getUsuario().getId(),"Pedido de troca criado","Acompanhe o envio. Esta troca não gera novos XP ou pontos.","/pedidos/"+novo.getId());return novo.getId();
 }
}
