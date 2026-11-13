package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.math.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The preview and execution use the same projection; no writes are made while previewing. */
@Service
public class ImpactoReembolsoService {
 private final PedidoRepository pedidos;
 private final PasseXpHistoricoRepository xp;
 private final PasseMissaoProgressoRepository missoes;
 private final VipMovimentoRepository vip;
 private final VipContaRepository contas;
 private final PasseRecompensaResgateRepository resgates;
 private final BauItemRepository bau;
 private final EventoSemanalService eventos;
 private final EventoResgateRepository selos;
 private final PasseService passe;
 public ImpactoReembolsoService(PedidoRepository p,PasseXpHistoricoRepository x,PasseMissaoProgressoRepository m,
 VipMovimentoRepository v,VipContaRepository c,PasseRecompensaResgateRepository r,BauItemRepository b,
 EventoSemanalService e,EventoResgateRepository s,PasseService ps) {pedidos=p;xp=x;missoes=m;vip=v;contas=c;resgates=r;bau=b;eventos=e;selos=s;passe=ps;}
 public record EventoImpacto(Long id,String titulo,int antes,int depois) {}
 public record Aviso(BigDecimal valor,long xp,long vip,List<EventoImpacto> eventos,boolean analisarBeneficios,String mensagem,String token) {}
 private record XpAjuste(PasseXpHistorico registro,int estorno,boolean cancelado) {}
 private record MissaoAjuste(PasseMissaoProgresso registro,int progresso,boolean completa) {}
 private record VipAjuste(VipMovimento registro,long estorno,boolean cancelado) {}
 private record Plano(List<XpAjuste> xp,List<MissaoAjuste> missoes,List<VipAjuste> vip,Aviso aviso) {}
 public static long proporcional(long pontos,BigDecimal devolvido,BigDecimal total) {
  return total.signum()==0 ? 0 : BigDecimal.valueOf(pontos).multiply(devolvido).divide(total,0,RoundingMode.DOWN).longValueExact();
 }
 @Transactional(readOnly=true)
 public Aviso prever(Pedido p,BigDecimal valor) {return plano(p,valor).aviso;}
 @Transactional
 public Aviso aplicar(Pedido p,BigDecimal valor,String token) {
  var plano=plano(p,valor);
  if(!plano.aviso.token.equals(token))throw new IllegalStateException("O progresso mudou. Revise o novo impacto e confirme novamente.");
  for(var a:plano.xp){a.registro.setXpEstornado(a.estorno);a.registro.setEstornado(a.cancelado);}
  for(var a:plano.missoes){a.registro.setProgressoAtual(a.progresso);a.registro.setConcluida(a.completa);a.registro.setRecompensaEntregue(a.completa);}
  for(var a:plano.vip){a.registro.pontosEstornados=a.estorno;a.registro.estornado=a.cancelado;}
  xp.flush();vip.flush();
  plano.xp.stream().map(a->a.registro.getTemporada()).distinct().forEach(t->passe.recalcularXp(p.getUsuario(),t));
  contas.findById(p.getUsuario().getId()).ifPresent(c->c.ultimaAtividade=vip.findByUsuarioIdAndEstornadoFalseOrderByCriadoEmDesc(c.usuarioId).stream()
   .filter(m->m.liquido()>0&&!m.criadoEm.isBefore(c.cicloInicio)).map(m->m.criadoEm).findFirst().orElse(null));
  return plano.aviso;
 }
 private Plano plano(Pedido p,BigDecimal valor) {
  if(valor==null||valor.signum()<0||valor.stripTrailingZeros().scale()>2||p.getValorReembolsado().add(valor).compareTo(p.getTotal())>0)
   throw new IllegalArgumentException("Valor excede o saldo reembolsável do pedido.");
  BigDecimal acumulado=p.getValorReembolsado().add(valor);
  Long usuario=p.getUsuario().getId();
  var xa=new ArrayList<XpAjuste>();var ma=new ArrayList<MissaoAjuste>();var va=new LinkedHashMap<Long,VipAjuste>();
  var compras=pedidos.findByUsuarioIdOrderByDataPedidoDesc(usuario).stream().filter(o->!o.isRecompensa()&&o.getPedidoOrigemId()==null&&o.getTemporadaPasseId()==null
   &&o.getStatus()!=Pedido.Status.CANCELADO&&o.getPagamento()!=null&&o.getPagamento().getStatus()==Pagamento.StatusPagamento.APROVADO).toList();
  var historicos=xp.findByUsuarioIdAndOrigemTipoAndReferencia(usuario,PasseOrigemXpTipo.PURCHASE,"PURCHASE-"+p.getId());
  for(var h:historicos) {
   xa.add(new XpAjuste(h,(int)proporcional(h.getXpGanho(),acumulado,p.getTotal()),acumulado.compareTo(p.getTotal())==0));
   for(var m:missoes.findByUsuarioIdAndTemporadaId(usuario,h.getTemporada().getId())) {
    var t=m.getTemplate();if(t.getAcaoTipo()!=PasseAcaoTipo.PURCHASE_APPROVED&&t.getAcaoTipo()!=PasseAcaoTipo.SPEND_APPROVED_CENTS)continue;
    var validas=compras.stream().filter(o->o.getPagamento().getPagoEm()!=null&&!o.getPagamento().getPagoEm().isBefore(m.getPeriodoInicio())&&o.getPagamento().getPagoEm().isBefore(m.getPeriodoFim()))
     .filter(o->o.valorElegivel(o.getId().equals(p.getId())?acumulado:o.getValorReembolsado()).signum()>0).toList();
    int progresso=t.getAcaoTipo()==PasseAcaoTipo.PURCHASE_APPROVED?Math.min(t.getMetaValor(),validas.size()):validas.stream()
     .map(o->o.valorElegivel(o.getId().equals(p.getId())?acumulado:o.getValorReembolsado())).reduce(BigDecimal.ZERO,BigDecimal::add)
     .multiply(BigDecimal.valueOf(100)).min(BigDecimal.valueOf(t.getMetaValor())).intValue();
    boolean completa=progresso>=t.getMetaValor();ma.add(new MissaoAjuste(m,progresso,completa));
    String ref="MISSION-"+t.getCodigo()+"-"+m.getPeriodoInicio();
    xp.findByUsuarioIdAndTemporadaIdAndOrigemTipoAndReferencia(usuario,h.getTemporada().getId(),PasseOrigemXpTipo.MISSION,ref)
     .ifPresent(r->xa.add(new XpAjuste(r,r.getXpEstornado(),!completa)));
    vip.findByUsuarioIdAndReferencia(usuario,"MISSAO-"+h.getTemporada().getId()+"-"+ref).ifPresent(v->va.put(v.id,new VipAjuste(v,v.pontosEstornados,!completa)));
   }
  }
  for(var v:vip.findByPedidoId(p.getId()))va.put(v.id,new VipAjuste(v,proporcional(v.pontos,acumulado,p.getTotal()),acumulado.compareTo(p.getTotal())==0));
  var eventoImpactos=eventos.impactos(p,acumulado);
  for(var e:eventoImpactos)if(e.antes()!=e.depois()) {
   vip.findByUsuarioIdAndReferencia(usuario,"EVENTO-"+e.id()).ifPresent(v->{boolean completo=eventos.metaAtingida(e.id(),e.depois());va.put(v.id,new VipAjuste(v,v.pontosEstornados,!completo));});
  }
  var conta=contas.findById(usuario).orElse(null);
  long perdaXp=xa.stream().mapToLong(a->a.registro.xpLiquido()-(a.cancelado?0:a.registro.getXpGanho()-a.estorno)).sum();
  long perdaVip=va.values().stream().filter(a->conta==null||!a.registro.criadoEm.isBefore(conta.cicloInicio))
   .mapToLong(a->a.registro.liquido()-(a.cancelado?0:a.registro.pontos-a.estorno)).sum();
  boolean analisar=(perdaXp>0&&!resgates.findByUsuarioId(usuario).isEmpty())
   ||(perdaVip>0&&!bau.findByUsuarioIdOrderByIdDesc(usuario).isEmpty())
   ||eventoImpactos.stream().anyMatch(e->e.antes()>e.depois()&&selos.existsByEventoIdAndUsuarioId(e.id(),usuario))
   ||(p.getTemporadaPasseId()!=null&&!resgates.findByUsuarioIdAndTemporadaId(usuario,p.getTemporadaPasseId()).isEmpty());
  long perdaEvento=eventoImpactos.stream().mapToLong(e->Math.max(0,e.antes()-e.depois())).sum();
  String mensagem="Caso seu reembolso seja confirmado, serão retirados "+Math.max(0,perdaXp)+" XP do histórico do passe, "+Math.max(0,perdaVip)+" pontos VIP do ciclo atual e "+perdaEvento+" pontos dos eventos afetados. Missões e níveis serão recalculados.";
  if(p.getTemporadaPasseId()!=null)mensagem+=" O reembolso integral também revoga o Premium desta temporada; não permite comprar o mesmo passe novamente.";
  if(analisar)mensagem+=" Benefícios já resgatados serão encaminhados para análise da equipe; nada será cobrado automaticamente.";
  // Include both current and projected values to reject a stale acknowledgment, even when the delta is unchanged.
  StringBuilder fingerprint=new StringBuilder(p.getId()+"|"+p.getValorReembolsado()+"|"+valor.setScale(2)+"|"+mensagem);
  xa.forEach(a->fingerprint.append("|x").append(a.registro.getId()).append(':').append(a.registro.xpLiquido()).append(':').append(a.estorno).append(':').append(a.cancelado));
  va.values().forEach(a->fingerprint.append("|v").append(a.registro.id).append(':').append(a.registro.liquido()).append(':').append(a.estorno).append(':').append(a.cancelado));
  fingerprint.append(eventoImpactos);
  String token;try{token=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fingerprint.toString().getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}
  return new Plano(xa,ma,new ArrayList<>(va.values()),new Aviso(valor,Math.max(0,perdaXp),Math.max(0,perdaVip),eventoImpactos,analisar,mensagem,token));
 }
}
