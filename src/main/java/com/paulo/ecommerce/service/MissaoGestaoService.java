package com.paulo.ecommerce.service;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class MissaoGestaoService {
 private final PasseMissaoTemplateRepository missoes;private final TemporadaEditorService editor;
 private final ProdutoRepository produtos;private final ProdutoService catalogo;private final CategoriaRepository categorias;private final EventoSemanalRepository eventos;private final PasseTemporadaRepository temporadas;
 public MissaoGestaoService(PasseMissaoTemplateRepository m,TemporadaEditorService e,ProdutoRepository p,ProdutoService c,CategoriaRepository cat,EventoSemanalRepository ev,PasseTemporadaRepository t){missoes=m;editor=e;produtos=p;catalogo=c;categorias=cat;eventos=ev;temporadas=t;}
 public boolean ativaHoje(PasseMissaoTemplate m){
  if(!disponivelEm(m,LocalDate.now()))return false;
  if(m.getEventoId()==null)return true;
  var e=eventos.findById(m.getEventoId()).orElseThrow();var agora=LocalDateTime.now();return !agora.isBefore(e.inicio)&&agora.isBefore(e.fim);
 }
 public boolean disponivelEm(PasseMissaoTemplate m,LocalDate dia){
  if(!m.disponivelEm(dia)||dia.isBefore(m.getTemporada().getInicioEm())||dia.isAfter(m.getTemporada().getFimEm()))return false;
  if(m.getEventoId()!=null){var ev=eventos.findById(m.getEventoId());if(ev.isEmpty()||!dia.plusDays(1).atStartOfDay().isAfter(ev.get().inicio)||!dia.atStartOfDay().isBefore(ev.get().fim))return false;}
  return true;
 }
 public boolean aceita(PasseMissaoTemplate m,String ref){
  if(!ativaHoje(m))return false;
  if(m.getEventoId()!=null){var ev=eventos.findById(m.getEventoId()).orElseThrow();var now=LocalDateTime.now();if(now.isBefore(ev.inicio)||!now.isBefore(ev.fim))return false;}
  if(m.getProdutoId()==null&&m.getCategoriaId()==null)return true;
  if(ref==null||!ref.matches("(?:produto|review)-[0-9]+"))return false;
  var id=Long.valueOf(ref.substring(ref.lastIndexOf('-')+1));var p=produtos.findById(id).orElse(null);if(p==null)return false;
  if(m.getProdutoId()!=null)return m.getProdutoId().equals(id);
  if(p.getProdutoPrincipalId()!=null)p=produtos.findById(p.getProdutoPrincipalId()).orElse(p);
  return p.getCategoria()!=null&&m.getCategoriaId().equals(p.getCategoria().getId());
 }
 public PasseMissaoTemplate buscar(Long temporada,Long id){var m=missoes.findById(id).orElseThrow();if(!m.getTemporada().getId().equals(temporada))throw new IllegalArgumentException("Missão de outra temporada.");return m;}
 public void salvar(Long temporada,Long id,String titulo,String descricao,PasseAcaoTipo acao,PassePeriodicidade periodo,int meta,int xp,boolean premium,boolean especial,int ordem,Long produto,Long categoria,Long evento,LocalDate inicio,LocalDate fim){
  var t=editor.rascunho(temporada);
  if(titulo==null||titulo.isBlank()||titulo.length()>120||descricao==null||descricao.isBlank()||descricao.length()>255||meta<1||meta>100||xp<1||xp>3000||ordem<0||ordem>100000||acao==null||periodo==null||!Set.of(PasseAcaoTipo.CHECKIN,PasseAcaoTipo.VIEW_PRODUCT,PasseAcaoTipo.ADD_TO_CART,PasseAcaoTipo.REVIEW_SUBMITTED).contains(acao)||premium&&acao==PasseAcaoTipo.REVIEW_SUBMITTED)throw new IllegalArgumentException("Confira a missão. Premium exige ações simples sem compra ou avaliação obrigatória.");
  if(produto!=null&&categoria!=null||acao==PasseAcaoTipo.CHECKIN&&(produto!=null||categoria!=null))throw new IllegalArgumentException("Escolha produto ou categoria; check-in não aceita esses filtros.");
  if(produto!=null&&!produtos.existsById(produto)||categoria!=null&&!categorias.existsById(categoria)||evento!=null&&!eventos.existsById(evento))throw new IllegalArgumentException("Confira o produto, categoria ou evento.");
  if(inicio==null)inicio=t.getInicioEm();if(fim==null)fim=t.getFimEm();
  if(inicio.isBefore(t.getInicioEm())||fim.isAfter(t.getFimEm())||fim.isBefore(inicio)||especial&&periodo!=PassePeriodicidade.MENSAL)throw new IllegalArgumentException("Use datas dentro da temporada. Missões especiais são únicas e usam o período mensal.");
  if(acao==PasseAcaoTipo.CHECKIN&&meta>1&&periodo==PassePeriodicidade.DIARIA||produto!=null&&meta>1&&periodo==PassePeriodicidade.DIARIA)throw new IllegalArgumentException("Esta ação só pode contar uma vez por dia. Ajuste a meta ou o período.");
  var m=id==null?new PasseMissaoTemplate():buscar(temporada,id);m.setTemporada(t);if(id==null)m.setCodigo("gestao-"+UUID.randomUUID());m.setTitulo(titulo.strip());m.setDescricao(descricao.strip());m.setAcaoTipo(acao);m.setPeriodicidade(periodo);m.setMetaValor(meta);m.setXpRecompensa(xp);m.setPremium(premium);m.setEspecial(especial);m.setOrdemExibicao(ordem);m.setProdutoId(produto);m.setCategoriaId(categoria);m.setEventoId(evento);m.setInicioEm(inicio);m.setFimEm(fim);missoes.save(m);
 }
 public void duplicar(Long temporada,Long id){editor.rascunho(temporada);var m=buscar(temporada,id);salvar(temporada,null,m.getTitulo().substring(0,Math.min(110,m.getTitulo().length()))+" (cópia)",m.getDescricao(),m.getAcaoTipo(),m.getPeriodicidade(),m.getMetaValor(),m.getXpRecompensa(),m.isPremium(),m.isEspecial(),Math.min(100000,m.getOrdemExibicao()+1),m.getProdutoId(),m.getCategoriaId(),m.getEventoId(),m.getInicioEm(),m.getFimEm());}
 public void ordem(Long temporada,Long id,int ordem){editor.rascunho(temporada);if(ordem<0||ordem>100000)throw new IllegalArgumentException("Ordem inválida.");buscar(temporada,id).setOrdemExibicao(ordem);}
 public record Simulacao(LocalDate entrada,LocalDate conclusao,long xpDisponivel,long xpNecessario,int dias,int excluidas){}
 public Simulacao simular(Long temporada,LocalDate entrada){
  var t=temporadas.findById(temporada).orElseThrow();if(entrada.isBefore(t.getInicioEm())||entrada.isAfter(t.getFimEm()))throw new IllegalArgumentException("A entrada precisa ocorrer durante a temporada.");
  var ms=missoes.findByTemporadaIdAndAtivaTrueOrderByPeriodicidadeAscOrdemExibicaoAsc(temporada);var contadores=new HashMap<String,Integer>();var concluidas=new HashSet<String>();long xp=0;LocalDate conclusao=null;
  var ps=produtos.findAll().stream().filter(catalogo::disponivelNaLoja).toList();int excluidas=(int)ms.stream().filter(m->!simulavel(m)).count();
  for(var dia=entrada;!dia.isAfter(t.getFimEm());dia=dia.plusDays(1))for(var m:ms){
   if(!simulavel(m)||!disponivelEm(m,dia))continue;
   var periodo=switch(m.getPeriodicidade()){case DIARIA->dia;case SEMANAL->dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));case MENSAL->dia.withDayOfMonth(1);};
   String key=m.getId()+":"+periodo;if(concluidas.contains(key))continue;
   long capacidade=m.getAcaoTipo()==PasseAcaoTipo.CHECKIN?1:ps.stream().filter(p->m.getProdutoId()==null||m.getProdutoId().equals(p.getId())).filter(p->m.getCategoriaId()==null||p.getCategoria()!=null&&m.getCategoriaId().equals(p.getCategoria().getId())).count();
   int atual=contadores.merge(key,(int)Math.min(capacidade,m.getMetaValor()),Integer::sum);
   if(atual>=m.getMetaValor()){concluidas.add(key);xp+=m.getXpRecompensa();if(conclusao==null&&xp>=(long)t.getTotalNiveis()*t.getXpPorNivel())conclusao=dia;}
  }
  return new Simulacao(entrada,conclusao,xp,(long)t.getTotalNiveis()*t.getXpPorNivel(),conclusao==null?0:(int)java.time.temporal.ChronoUnit.DAYS.between(entrada,conclusao)+1,excluidas);
 }
 private boolean simulavel(PasseMissaoTemplate m){return Set.of(PasseAcaoTipo.CHECKIN,PasseAcaoTipo.VIEW_PRODUCT,PasseAcaoTipo.ADD_TO_CART).contains(m.getAcaoTipo());}
}
