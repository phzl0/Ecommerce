package com.paulo.ecommerce.service;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.util.*;
import java.time.LocalDateTime;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class ExpedicaoService {
 private final PedidoService pedidos;
 private final PedidoExpedicaoRepository expedicoes;
 private final PedidoMovimentacaoRepository historico;
 private final NotificacaoService notificacoes;
 public ExpedicaoService(PedidoService p,PedidoExpedicaoRepository e,PedidoMovimentacaoRepository h,NotificacaoService n) {pedidos=p;expedicoes=e;historico=h;notificacoes=n;}
 private Pedido preparar(Long id) {
  var p=pedidos.bloquearPedido(id);
  if(p.getStatus()!=Pedido.Status.CONFIRMADO || p.getTemporadaPasseId()!=null || p.getItens().stream().noneMatch(i->i.getTipo()==Produto.Tipo.FISICO)
     || (!p.isRecompensa() && (p.getPagamento()==null || p.getPagamento().getStatus()!=Pagamento.StatusPagamento.APROVADO)))
    throw new IllegalStateException("A separação exige um pedido físico confirmado.");
  return p;
 }
 public void iniciar(Long id) {
  var p=preparar(id);
  if(expedicoes.existsById(id)) return;
  var e=new PedidoExpedicao();e.pedidoId=id;expedicoes.save(e);
  historico.save(new PedidoMovimentacao(id,p.getStatus(),"Separação iniciada"));
 }
 public void conferir(Long id,Map<Long,Integer> quantidades,String autor) {
  var p=preparar(id);var e=expedicoes.findById(id).orElseThrow(()->new IllegalStateException("Inicie a separação primeiro."));
  var fisicos=p.getItens().stream().filter(i->i.getTipo()==Produto.Tipo.FISICO).toList();
  if(quantidades.size()!=fisicos.size() || fisicos.stream().anyMatch(i->!i.getQuantidade().equals(quantidades.get(i.getId()))))
    throw new IllegalArgumentException("Confira a quantidade exata de cada item físico do pedido.");
  if("CONFERIDO".equals(e.etapa)) return;
  if(!"SEPARANDO".equals(e.etapa)) throw new IllegalStateException("Etapa não permite conferência.");
  e.etapa="CONFERIDO";e.conferidoEm=LocalDateTime.now();e.conferidoPor=autor;
  historico.save(new PedidoMovimentacao(id,p.getStatus(),"Itens físicos conferidos para envio"));
 }
 public void enviar(Long id,String transportadora,String codigo,String url) {
  var p=pedidos.bloquearPedido(id);var e=expedicoes.findById(id).orElseThrow(()->new IllegalStateException("Separe e confira os itens antes de enviar."));
  transportadora=texto(transportadora,100);codigo=texto(codigo,80);url=urlSegura(url);
  if(p.getStatus()==Pedido.Status.ENVIADO || p.getStatus()==Pedido.Status.ENTREGUE) {
    if(Objects.equals(transportadora,e.transportadora)&&Objects.equals(codigo,e.codigo)&&Objects.equals(url,e.url)) return;
    throw new IllegalStateException("Use a atualização de rastreamento para corrigir os dados.");
  }
  preparar(id);
  if(!"CONFERIDO".equals(e.etapa)) throw new IllegalStateException("Conclua a conferência dos itens antes de enviar.");
  e.transportadora=transportadora;e.codigo=codigo;e.url=url;
  pedidos.atualizarStatusOperacional(id,Pedido.Status.ENVIADO);
 }
 public void rastreio(Long id,String transportadora,String codigo,String url) {
  var p=pedidos.bloquearPedido(id);var e=expedicoes.findById(id).orElseThrow();
  if(p.getStatus()!=Pedido.Status.ENVIADO && p.getStatus()!=Pedido.Status.ENTREGUE) throw new IllegalStateException("Envie o pedido antes de atualizar o rastreio.");
  transportadora=texto(transportadora,100);codigo=texto(codigo,80);url=urlSegura(url);
  if(Objects.equals(e.transportadora,transportadora)&&Objects.equals(e.codigo,codigo)&&Objects.equals(e.url,url)) return;
  e.transportadora=transportadora;e.codigo=codigo;e.url=url;
  historico.save(new PedidoMovimentacao(id,p.getStatus(),"Rastreamento atualizado"));
  notificacoes.registrar(p.getUsuario().getId(),"Rastreamento atualizado","Pedido "+p.getNumero(),"/pedidos/"+id);
 }
 public void entregar(Long id) {
  var p=pedidos.bloquearPedido(id);if(p.getStatus()==Pedido.Status.ENTREGUE)return;
  pedidos.atualizarStatusOperacional(id,Pedido.Status.ENTREGUE);
 }
 private String texto(String valor,int limite) {
  if(valor==null || valor.strip().isEmpty() || valor.length()>limite || valor.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Informe transportadora e código válidos.");
  return valor.strip();
 }
 public static String urlSegura(String valor) {
  if(valor==null || valor.isBlank()) return null;
  try {
   var uri=URI.create(valor.strip());String host=uri.getHost();
   if(valor.length()>1000 || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo()!=null || host==null || !host.matches("(?i)[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.[a-z]{2,}") || host.toLowerCase(Locale.ROOT).endsWith(".localhost") || uri.getPort()!=-1)
    throw new IllegalArgumentException();
   if(uri.toASCIIString().length()>1000)throw new IllegalArgumentException();
   return uri.toASCIIString();
  } catch(IllegalArgumentException e) {throw new IllegalArgumentException("Informe um link HTTPS de rastreamento válido, sem credenciais ou porta.");}
 }
}
