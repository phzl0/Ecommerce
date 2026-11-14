package com.paulo.ecommerce.service;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.math.*;
import java.util.*;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.env.Environment;
@Service
@Transactional
public class ReembolsoService {
 @org.springframework.beans.factory.annotation.Autowired private PedidoRepository repo;
 private final PedidoService pedidos;
 private final PedidoReembolsoRepository reembolsos;
 private final ImpactoReembolsoService impactos;
 private final PedidoMovimentacaoRepository historico;
 private final NotificacaoService notificacoes;
 private final Environment ambiente;
 public ReembolsoService(PedidoService p,PedidoReembolsoRepository r,ImpactoReembolsoService i,PedidoMovimentacaoRepository h,NotificacaoService n,Environment e){pedidos=p;reembolsos=r;impactos=i;historico=h;notificacoes=n;ambiente=e;}
 public boolean simulacaoDisponivel(){return ambiente.matchesProfiles("local & !prod")&&ambiente.getProperty("app.local-demo-enabled",Boolean.class,false)&&!ambiente.getProperty("app.payments.enabled",Boolean.class,false);}
 private void elegivel(Pedido p){
  if(repo.findByPedidoOrigemId(p.getId()).stream().anyMatch(o->o.getStatus()!=Pedido.Status.CANCELADO))throw new IllegalStateException("Há uma remessa de troca vinculada. Resolva a troca antes de reembolsar o pedido original.");
  if(p.isRecompensa()||p.getPedidoOrigemId()!=null||p.getTotal().signum()<=0||p.getPagamento()==null||p.getPagamento().getStatus()!=Pagamento.StatusPagamento.APROVADO)
   throw new IllegalStateException("O pedido não tem pagamento aprovado reembolsável.");
 }
 public ImpactoReembolsoService.Aviso prever(Long id,BigDecimal valor){var p=pedidos.buscarPorId(id);elegivel(p);return impactos.prever(p,valor);}
 public Long solicitar(Long id,BigDecimal valor,String motivo,String chave,String token,boolean ciente,String autor){
  var p=pedidos.bloquearPedidoParaConciliacao(id);
  var existente=reembolsos.findByPedidoIdAndChave(id,chave);if(existente.isPresent())return existente.get().id;
  elegivel(p);validar(motivo,5,1000);validar(chave,8,80);
  if(valor==null||valor.signum()<=0)throw new IllegalArgumentException("Informe um valor positivo.");
  var reservado=reembolsos.findByPedidoIdOrderByIdDesc(id).stream().filter(r->"PENDENTE".equals(r.estado)).map(r->r.valor).reduce(BigDecimal.ZERO,BigDecimal::add);
  if(reservado.add(valor).add(p.getValorReembolsado()).compareTo(p.getTotal())>0)throw new IllegalStateException("Há reembolsos pendentes ou confirmados que já comprometem este saldo.");
  var aviso=impactos.prever(p,valor);confirmarAviso(aviso,token,ciente);
  var r=new PedidoReembolso();r.pedidoId=id;r.valor=valor;r.motivo=motivo.strip();r.chave=chave;r.solicitadoPor=autor;r.impacto=aviso.mensagem();
  reembolsos.save(r);historico.save(new PedidoMovimentacao(id,p.getStatus(),"Reembolso solicitado #"+r.id+" · R$ "+valor+" · "+autor));
  notificacoes.registrar(p.getUsuario().getId(),"Reembolso em análise","Pedido "+p.getNumero()+". Nenhum ponto foi retirado nesta solicitação.","/pedidos/"+id);return r.id;
 }
 public void confirmarSimulado(Long pedidoId,Long id,String token,boolean ciente,String autor){
  if(!simulacaoDisponivel())throw new IllegalStateException("Confirmação simulada disponível somente na demonstração local. Integração real pendente.");
  var p=pedidos.bloquearPedidoParaConciliacao(pedidoId);var r=obter(pedidoId,id);
  if("CONFIRMADO".equals(r.estado))return;
  if(!"PENDENTE".equals(r.estado))throw new IllegalStateException("Reembolso não está pendente.");
  elegivel(p);
  if(p.getPagamento().getMpPaymentId()!=null||p.getPagamento().getMpPreferenceId()!=null)throw new IllegalStateException("Um pagamento do provedor não pode ser estornado por simulação.");
  var aviso=impactos.prever(p,r.valor);confirmarAviso(aviso,token,ciente);impactos.aplicar(p,r.valor,token);
  p.setValorReembolsado(p.getValorReembolsado().add(r.valor));
  if(p.getValorReembolsado().compareTo(p.getTotal())==0){p.getPagamento().setStatus(Pagamento.StatusPagamento.REEMBOLSADO);pedidos.atualizarStatus(pedidoId,Pedido.Status.CANCELADO);}
  r.estado="CONFIRMADO";r.confirmadoPor=autor;r.confirmadoEm=LocalDateTime.now();r.impacto=aviso.mensagem().replace("Caso seu reembolso seja confirmado, serão retirados", "Reembolso confirmado. Foram retirados");r.analiseBeneficios=aviso.analisarBeneficios();
  historico.save(new PedidoMovimentacao(pedidoId,p.getStatus(),"Reembolso simulado confirmado #"+id+" · R$ "+r.valor+" · "+autor));
  notificacoes.registrar(p.getUsuario().getId(),"Reembolso simulado confirmado","Foram retirados "+aviso.xp()+" XP, "+aviso.vip()+" pontos VIP e os pontos dos eventos indicados no pedido. Consulte os detalhes.","/pedidos/"+pedidoId);
 }
 public void cancelar(Long pedidoId,Long id,String autor){var p=pedidos.bloquearPedidoParaConciliacao(pedidoId);var r=obter(pedidoId,id);if("CANCELADO".equals(r.estado))return;if(!"PENDENTE".equals(r.estado))throw new IllegalStateException("Só solicitações pendentes podem ser canceladas.");r.estado="CANCELADO";historico.save(new PedidoMovimentacao(pedidoId,p.getStatus(),"Solicitação de reembolso cancelada #"+id+" · "+autor));}
 public void analisar(Long pedidoId,Long id,String resolucao,String autor){var p=pedidos.bloquearPedidoParaConciliacao(pedidoId);var r=obter(pedidoId,id);validar(resolucao,10,1500);if(!r.analiseBeneficios||!"CONFIRMADO".equals(r.estado))throw new IllegalStateException("Sem análise de benefícios pendente.");if(r.analisadoEm!=null)return;r.resolucaoAnalise=resolucao.strip();r.analisadoPor=autor;r.analisadoEm=LocalDateTime.now();historico.save(new PedidoMovimentacao(pedidoId,p.getStatus(),"Análise de benefícios registrada para reembolso #"+id+" · "+autor));}
 private PedidoReembolso obter(Long pedidoId,Long id){var r=reembolsos.findById(id).orElseThrow();if(!r.pedidoId.equals(pedidoId))throw new IllegalArgumentException("Reembolso não pertence ao pedido.");return r;}
 private void confirmarAviso(ImpactoReembolsoService.Aviso a,String token,boolean ciente){if(!ciente)throw new IllegalArgumentException("Confirme a leitura do impacto no progresso.");if(!a.token().equals(token))throw new IllegalStateException("O progresso mudou. Revise o impacto antes de confirmar.");}
 private void validar(String texto,int min,int max){if(texto==null||texto.strip().length()<min||texto.length()>max)throw new IllegalArgumentException("Confira o motivo ou identificador da operação.");}
}
