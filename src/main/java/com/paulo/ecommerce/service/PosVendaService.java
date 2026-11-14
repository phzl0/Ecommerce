package com.paulo.ecommerce.service;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.util.*;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
@Service
@Transactional
public class PosVendaService {
 @org.springframework.beans.factory.annotation.Autowired private DigitalEntregaRepository digitais;
 @org.springframework.beans.factory.annotation.Autowired private PedidoReembolsoRepository reembolsos;
 @org.springframework.beans.factory.annotation.Autowired private PedidoRepository pedidoRepo;
 @org.springframework.beans.factory.annotation.Autowired private ImpactoReembolsoService impactos;
 public ImpactoReembolsoService.Aviso preverSolicitacao(Long id,String email,Long itemId,int quantidade) {
  var p=pedidos.buscarPorIdDoUsuario(id,email);
  var item=p.getItens().stream().filter(i->i.getId().equals(itemId)).findFirst().orElseThrow(()->new IllegalArgumentException("Item não pertence ao pedido."));
  if(quantidade<1||quantidade>item.getQuantidade())throw new IllegalArgumentException("Quantidade inválida.");
  var bruto=item.getPrecoUnitario().multiply(java.math.BigDecimal.valueOf(quantidade));
  var valor=p.getSubtotal().signum()==0?java.math.BigDecimal.ZERO:bruto.multiply(p.getSubtotal().subtract(p.getDesconto())).divide(p.getSubtotal(),2,java.math.RoundingMode.DOWN);
  valor=valor.min(p.getTotal().subtract(p.getValorReembolsado()));
  return impactos.prever(p,valor);
 }
 public Long solicitarComAviso(Long pedido,String email,Long item,int quantidade,String tipo,String motivo,String chave,String token,boolean ciente){
  pedidos.bloquearPedidoParaConciliacao(pedido);
  if(!ciente)throw new IllegalArgumentException("Leia e confirme o aviso antes de solicitar.");
  var aviso=preverSolicitacao(pedido,email,item,quantidade);
  if(!aviso.token().equals(token))throw new IllegalStateException("O progresso mudou. Atualize o aviso antes de enviar.");
  return solicitar(pedido,email,item,quantidade,tipo,motivo,chave);
 }
 private final PedidoService pedidos;
 private final PedidoDevolucaoRepository devolucoes;
 private final PedidoAtendimentoRepository mensagens;
 private final PedidoExpedicaoRepository expedicoes;
 private final ProdutoRepository produtos;
 private final EstoqueService estoque;
 private final NotificacaoService notificacoes;
 public PosVendaService(PedidoService p,PedidoDevolucaoRepository d,PedidoAtendimentoRepository m,PedidoExpedicaoRepository e,ProdutoRepository pr,EstoqueService s,NotificacaoService n) {
  pedidos=p;devolucoes=d;mensagens=m;expedicoes=e;produtos=pr;estoque=s;notificacoes=n;
 }
 private Pedido cliente(Long id,String email) {
  var p=pedidos.bloquearPedido(id);
  if(!p.getUsuario().getEmail().equalsIgnoreCase(email))throw new AccessDeniedException("Pedido pertence a outra conta.");
  return p;
 }
 public void mensagemCliente(Long pedido,String email,String texto,String chave) {
  var p=cliente(pedido,email);validar(chave,8,72,"identificador");mensagem(p,texto,"CLI:"+chave,email,true,true);
 }
 public void mensagemAdmin(Long pedido,String autor,String texto,String chave,boolean publico) {
  var p=pedidos.bloquearPedido(pedido);validar(chave,8,72,"identificador");mensagem(p,texto,"ADM:"+chave,autor,publico,false);
 }
 private void mensagem(Pedido p,String texto,String chave,String autor,boolean publico,boolean cliente) {
  validar(texto,2,2000,"mensagem");validar(chave,8,80,"identificador");
  if(mensagens.findByPedidoIdAndChave(p.getId(),chave).isPresent()) return;
  var m=new PedidoAtendimento();m.pedidoId=p.getId();m.chave=chave;m.mensagem=texto.strip();m.autor=autor;m.publico=publico;m.cliente=cliente;mensagens.save(m);
  if(publico&&!cliente)notificar(p,"Nova resposta no atendimento");
 }
 public Long solicitar(Long pedido,String email,Long itemId,int quantidade,String tipo,String motivo,String chave) {
  var p=cliente(pedido,email);validar(chave,8,80,"identificador");validar(motivo,10,1000,"motivo");
  if(!Set.of("TROCA","DEVOLUCAO").contains(tipo))throw new IllegalArgumentException("Escolha troca ou devolução.");
  var existente=devolucoes.findByPedidoIdAndChave(pedido,chave);if(existente.isPresent())return existente.get().id;
  var item=p.getItens().stream().filter(i->i.getId().equals(itemId)).findFirst().orElseThrow(()->new IllegalArgumentException("Item não pertence ao pedido."));
  var envio=expedicoes.findById(pedido).orElse(null);
  if(item.getTipo()!=Produto.Tipo.FISICO || envio==null || !Set.of("ENVIADO","ENTREGUE").contains(envio.etapa))
    throw new IllegalArgumentException("Solicitações de produtos físicos ficam disponíveis após o envio. Para outros casos, use o atendimento.");
  if(quantidade<1 || quantidade>item.getQuantidade()-devolucoes.quantidadeSolicitada(itemId))
    throw new IllegalArgumentException("Quantidade excede os itens disponíveis para solicitação.");
  var d=new PedidoDevolucao();d.pedidoId=pedido;d.itemId=itemId;d.quantidade=quantidade;d.tipo=tipo;d.motivo=motivo.strip();d.chave=chave;devolucoes.save(d);
  notificar(p,"Solicitação recebida");return d.id;
 }
 @org.springframework.cache.annotation.CacheEvict(value={"produto","produtos","metricas"},allEntries=true)
 public void atualizar(Long pedido,Long id,String acao,int repor,String resposta,String autor) {
  var p=pedidos.bloquearPedido(pedido);var d=devolucoes.findById(id).orElseThrow();
  if(!d.pedidoId.equals(pedido))throw new IllegalArgumentException("Solicitação não pertence ao pedido.");
  validar(resposta,5,1000,"resposta ao cliente");
  String destino=switch(acao){case "autorizar"->"AUTORIZADA";case "recusar"->"RECUSADA";case "receber"->"RECEBIDA";case "concluir"->"CONCLUIDA";default->throw new IllegalArgumentException("Ação inválida.");};
  if(d.estado.equals(destino))return;
  boolean permitido=switch(acao){case "autorizar","recusar"->"SOLICITADA".equals(d.estado);case "receber"->"AUTORIZADA".equals(d.estado);case "concluir"->"RECEBIDA".equals(d.estado);default->false;};
  if(!permitido)throw new IllegalStateException("Ação não disponível para esta etapa.");
  if("receber".equals(acao)) {
   if(repor<0 || repor>d.quantidade)throw new IllegalArgumentException("Informe entre zero e a quantidade devolvida para reposição.");
   var item=p.getItens().stream().filter(i->i.getId().equals(d.itemId)).findFirst().orElseThrow();
   if(item.getTipo()!=Produto.Tipo.FISICO)throw new IllegalStateException("Somente produtos físicos podem voltar ao estoque.");
   var produto=produtos.findLockedById(item.getProduto().getId()).orElseThrow();
   produto.setEstoque(Math.addExact(produto.getEstoque(),repor));
   estoque.registrar(produto,repor,"DEVOLUCAO","Conferência da solicitação #"+id,autor);
   d.reposta=repor;d.recebidoEm=LocalDateTime.now();d.conferidoPor=autor;
  }
  d.estado=destino;d.resposta=resposta.strip();
  // Append each public decision instead of losing prior answers on the request.
  mensagem(p,"Solicitação #"+id+" · "+destino+"\n"+resposta,"DEV:"+id+":"+destino,autor,true,false);
 }
 private void notificar(Pedido p,String titulo) {notificacoes.registrar(p.getUsuario().getId(),titulo,"Pedido "+p.getNumero(),"/pedidos/"+p.getId());}
 private void validar(String v,int min,int max,String campo) {
  if(v==null || v.strip().length()<min || v.length()>max)throw new IllegalArgumentException("Confira o campo "+campo+" ("+min+" a "+max+" caracteres).");
 }
 public record Rastreio(String etapa,String transportadora,String codigo,String url,LocalDateTime enviadoEm,LocalDateTime entregueEm) {}
 public record MensagemView(Long id,String mensagem,String autor,LocalDateTime criadoEm) {}
 public record SolicitacaoView(Long id,Long itemId,int quantidade,String tipo,String estado,String motivo,String resposta,int reposta,Long pedidoTrocaId) {}
 public record ItemView(Long id,String nome,int quantidade,long disponivel) {}
 @Transactional(readOnly=true)
 public Map<String,Object> acompanhar(Long id,String email) {
  var p=pedidos.buscarPorIdDoUsuario(id,email);var e=expedicoes.findById(id).orElse(null);
  boolean fisico=p.getTemporadaPasseId()==null&&p.getItens().stream().anyMatch(i->i.getTipo()==Produto.Tipo.FISICO);
  String etapa=e==null?(fisico?(p.getStatus()==Pedido.Status.CONFIRMADO?"NA_FILA":p.getStatus().name()):"SEM_ENVIO"):e.etapa;
  if(p.getStatus()==Pedido.Status.CANCELADO && (e==null || e.enviadoEm==null) && !Set.of("ENVIADO","ENTREGUE").contains(etapa))etapa="CANCELADO";
  var rastreio=new Rastreio(etapa,e==null?null:e.transportadora,e==null?null:e.codigo,e==null?null:e.url,e==null?null:e.enviadoEm,e==null?null:e.entregueEm);
  var mapa=new LinkedHashMap<String,Object>();mapa.put("rastreio",rastreio);mapa.put("fisico",fisico);
  mapa.put("mensagens",mensagens.findTop100ByPedidoIdAndPublicoTrueOrderByIdDesc(id).stream().map(m->new MensagemView(m.id,m.mensagem,m.cliente?"Você":"Atendimento LEVEL",m.criadoEm)).toList());
  mapa.put("solicitacoes",devolucoes.findByPedidoIdOrderByIdDesc(id).stream().map(d->new SolicitacaoView(d.id,d.itemId,d.quantidade,d.tipo,d.estado,d.motivo,d.resposta,d.reposta,d.pedidoTrocaId)).toList());
  mapa.put("itens",p.getItens().stream().filter(i->i.getTipo()==Produto.Tipo.FISICO && p.getTemporadaPasseId()==null).map(i->new ItemView(i.getId(),i.getNomeSnapshot(),i.getQuantidade(),Math.max(0,i.getQuantidade()-devolucoes.quantidadeSolicitada(i.getId())))).toList());
  mapa.put("digitais",digitais.findByPedidoIdOrderByIdAsc(id).stream().map(d->Map.of("titulo",d.titulo,"estado",d.estado)).toList());
  mapa.put("pedidoOrigemId",p.getPedidoOrigemId());
  mapa.put("trocas",pedidoRepo.findByPedidoOrigemId(id).stream().map(o->Map.of("id",o.getId(),"numero",o.getNumero(),"status",o.getStatus())).toList());
  mapa.put("valorReembolsado",p.getValorReembolsado());
  mapa.put("reembolsos",reembolsos.findByPedidoIdOrderByIdDesc(id).stream().map(r->Map.of("id",r.id,"valor",r.valor,"estado",r.estado,"impacto",r.impacto,"analiseBeneficios",r.analiseBeneficios&&r.analisadoEm==null)).toList());
  mapa.put("podeSolicitar",e!=null && Set.of("ENVIADO","ENTREGUE").contains(e.etapa));return mapa;
 }
}
