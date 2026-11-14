package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class TemporadaEditorService {
 private final PasseTemporadaRepository temporadas; private final PasseRecompensaNivelRepository premios;
 private final RecompensaOpcaoRepository opcoes; private final ProdutoRepository produtos;
 private final PasseMissaoTemplateRepository missoes; private final FidelidadeConfigRepository config;
 public TemporadaEditorService(PasseTemporadaRepository t,PasseRecompensaNivelRepository r,RecompensaOpcaoRepository o,ProdutoRepository p,PasseMissaoTemplateRepository m,FidelidadeConfigRepository c){temporadas=t;premios=r;opcoes=o;produtos=p;missoes=m;config=c;}
 public PasseTemporada criar(String nome,LocalDate inicio,LocalDate fim,BigDecimal preco,int xp,int gratuitos,int premium){
  validar(nome,inicio,fim,preco,xp,gratuitos,premium);
  if(temporadas.findByAnoAndMes(inicio.getYear(),inicio.getMonthValue()).isPresent())throw new IllegalArgumentException("Já existe uma temporada neste mês. Edite o rascunho existente.");
  var t=new PasseTemporada();t.setGerenciada(true);t.setEstado("RASCUNHO");t.setAtiva(false);t.setSlug("passe-"+YearMonth.from(inicio));t.setAno(inicio.getYear());t.setMes(inicio.getMonthValue());t.setTotalNiveis(30);
  preencher(t,nome,inicio,fim,preco,xp,gratuitos,premium);return temporadas.save(t);
 }
 private void validar(String n,LocalDate a,LocalDate b,BigDecimal p,int xp,int g,int v){
  if(n==null||n.isBlank()||n.length()>120||a==null||b==null||b.isBefore(a)||!YearMonth.from(a).equals(YearMonth.from(b))||p==null||p.signum()<=0||p.scale()>2||p.compareTo(new BigDecimal("9999.99"))>0||xp<1||xp>10000||g<0||v<1||g>100000||v>100000)throw new IllegalArgumentException("Confira nome, período dentro do mesmo mês, preço, XP e previsão (ao menos um Premium).");
 }
 private void preencher(PasseTemporada t,String n,LocalDate a,LocalDate b,BigDecimal p,int xp,int g,int v){t.setNome(n.strip());t.setInicioEm(a);t.setFimEm(b);t.setPreco(p);t.setXpPorNivel(xp);t.setPrevisaoGratuitos(g);t.setPrevisaoPremium(v);}
 public PasseTemporada rascunho(Long id){var t=temporadas.bloquear(id).orElseThrow();if(!t.isGerenciada()||!"RASCUNHO".equals(t.getEstado()))throw new IllegalStateException("Somente rascunhos podem ser alterados. Temporadas publicadas preservam as regras oferecidas.");return t;}
 public void salvar(Long id,String nome,LocalDate inicio,LocalDate fim,BigDecimal preco,int xp,int g,int v){var t=rascunho(id);validar(nome,inicio,fim,preco,xp,g,v);if(inicio.getYear()!=t.getAno()||inicio.getMonthValue()!=t.getMes())throw new IllegalArgumentException("Crie outra temporada para mudar o mês.");preencher(t,nome,inicio,fim,preco,xp,g,v);}
 public void exemplos(Long id){
  rascunho(id);
  for(int n=1;n<=30;n++){
   final int nivel=n;
   var existentes=premios.findByTemporadaIdOrderByNivelAsc(id);
   if(existentes.stream().noneMatch(r->r.getNivel()==nivel&&r.isPremium()))premio(id,n,true,"Prêmio Premium demo · nível "+n,"Selo de demonstração. Configure seu prêmio antes da publicação.","Badge");
  }
  if(premios.findByTemporadaIdOrderByNivelAsc(id).stream().noneMatch(r->r.getNivel()==30&&!r.isPremium()))premio(id,30,false,"Prêmio final gratuito demo","Selo de demonstração do nível 30.","Badge");
 }
 public void premio(Long id,int nivel,boolean premium,String titulo,String descricao,String tipo){
  var t=rascunho(id);if(nivel<1||nivel>30||titulo==null||titulo.isBlank()||titulo.length()>120||descricao==null||descricao.isBlank()||descricao.length()>255||!Set.of("Fisico","Avatar","Wallpaper","Badge","Tema").contains(tipo))throw new IllegalArgumentException("Confira nível, título, descrição e tipo do prêmio.");
  var r=premios.findByTemporadaIdOrderByNivelAsc(id).stream().filter(x->x.getNivel()==nivel&&x.isPremium()==premium).findFirst().orElseGet(PasseRecompensaNivel::new);
  if(r.getId()!=null&&!tipo.equals("Fisico")&&!opcoes.findByRecompensaIdOrderByIdAsc(r.getId()).isEmpty())throw new IllegalStateException("Remova as opções físicas antes de mudar o tipo.");
  r.setTemporada(t);r.setNivel(nivel);r.setPremium(premium);r.setTitulo(titulo.strip());r.setDescricao(descricao.strip());r.setTipoDigital(tipo);r.setCodigo("LEVEL-"+id+"-"+nivel+"-"+premium);premios.save(r);
 }
 public void opcao(Long id,Long premioId,Long produtoId,BigDecimal custo,BigDecimal frete){
  rascunho(id);var r=premios.findById(premioId).orElseThrow();if(!r.getTemporada().getId().equals(id)||!"Fisico".equals(r.getTipoDigital()))throw new IllegalArgumentException("Escolha uma recompensa física desta temporada.");
  if(custo==null||frete==null||custo.signum()<0||frete.signum()<0||custo.scale()>2||frete.scale()>2||custo.compareTo(new BigDecimal("99999.99"))>0||frete.compareTo(new BigDecimal("99999.99"))>0)throw new IllegalArgumentException("Informe custos válidos.");
  var lista=opcoes.findByRecompensaIdOrderByIdAsc(premioId);var o=lista.stream().filter(x->x.produto.getId().equals(produtoId)).findFirst().orElse(null);
  if(o==null&&lista.size()>=2)throw new IllegalArgumentException("Cada prêmio aceita até duas opções.");
  var p=produtos.findById(produtoId).orElseThrow();if(p.getTipo()!=Produto.Tipo.FISICO)throw new IllegalArgumentException("Escolha um SKU físico.");
  if(o==null)o=new RecompensaOpcao();o.recompensa=r;o.produto=p;o.custo=custo;o.frete=frete;opcoes.save(o);
 }
 public void removerOpcao(Long id,Long opcao){rascunho(id);var o=opcoes.findById(opcao).orElseThrow();if(!o.recompensa.getTemporada().getId().equals(id))throw new IllegalArgumentException("Opção de outra temporada.");opcoes.delete(o);}
 public void missao(Long id,String titulo,PasseAcaoTipo acao,PassePeriodicidade periodo,int meta,int xp,boolean premium){
  var t=rascunho(id);if(titulo==null||titulo.isBlank()||titulo.length()>120||acao==null||periodo==null||meta<1||meta>100||xp<1||xp>3000||premium&&!Set.of(PasseAcaoTipo.CHECKIN,PasseAcaoTipo.VIEW_PRODUCT,PasseAcaoTipo.ADD_TO_CART).contains(acao))throw new IllegalArgumentException("Confira a missão. Premium exige ações simples, sem gasto.");
  var m=new PasseMissaoTemplate();m.setTemporada(t);m.setCodigo("editor-"+UUID.randomUUID());m.setTitulo(titulo.strip());m.setDescricao(titulo.strip());m.setAcaoTipo(acao);m.setPeriodicidade(periodo);m.setMetaValor(meta);m.setXpRecompensa(xp);m.setPremium(premium);m.setOrdemExibicao(100);missoes.save(m);
 }
 public void removerMissao(Long id,Long missao){rascunho(id);var m=missoes.findById(missao).orElseThrow();if(!m.getTemporada().getId().equals(id))throw new IllegalArgumentException("Missão de outra temporada.");missoes.delete(m);}
 public record Linha(PasseRecompensaNivel premio,List<RecompensaOpcao> opcoes,BigDecimal custoUnitario,BigDecimal custoPrevisto){}
 public record Previa(List<Linha> linhas,List<String> pendencias,BigDecimal custo,BigDecimal receita,BigDecimal saldo){}
 public Previa previa(Long id){
  var t=temporadas.findById(id).orElseThrow();var erros=new ArrayList<String>();var linhas=new ArrayList<Linha>();var demanda=new TreeMap<Long,Long>();BigDecimal total=BigDecimal.ZERO;
  var rs=premios.findByTemporadaIdOrderByNivelAsc(id);
  for(boolean premium:new boolean[]{false,true})if(rs.stream().noneMatch(r->r.getNivel()==30&&r.isPremium()==premium))erros.add("Configure a recompensa final do nível 30 na trilha "+(premium?"Premium":"Gratuita")+".");
  if(rs.stream().filter(PasseRecompensaNivel::isPremium).count()<30)erros.add("Configure os 30 níveis da trilha Premium.");
  for(var r:rs){var os=opcoes.findByRecompensaIdOrderByIdAsc(r.getId());BigDecimal custo=BigDecimal.ZERO;long pessoas=r.isPremium()?t.getPrevisaoPremium():(long)t.getPrevisaoPremium()+t.getPrevisaoGratuitos();
   if("Fisico".equals(r.getTipoDigital())){
    if(os.isEmpty())erros.add("Prêmio físico sem opção: "+r.getTitulo());
    for(var o:os){custo=custo.max(o.custo.add(o.frete));demanda.merge(o.produto.getId(),pessoas,Long::sum);}
   }
   var previsto=custo.multiply(BigDecimal.valueOf(pessoas));total=total.add(previsto);linhas.add(new Linha(r,os,custo,previsto));
  }
  for(var e:demanda.entrySet()){var p=produtos.findLockedById(e.getKey()).orElseThrow();if(p.getEstoque()<e.getValue())erros.add("Estoque insuficiente: "+p.nomeCompra()+" · disponível "+p.getEstoque()+", cenário conservador "+e.getValue());}
  var ms=missoes.findByTemporadaIdAndAtivaTrueOrderByPeriodicidadeAscOrdemExibicaoAsc(id);
  for(var m:ms)if(m.getInicioEm()!=null&&m.getInicioEm().isBefore(t.getInicioEm())||m.getFimEm()!=null&&m.getFimEm().isAfter(t.getFimEm()))erros.add("Datas da missão fora da temporada: "+m.getTitulo());
  if(ms.stream().noneMatch(PasseMissaoTemplate::isPremium))erros.add("Inclua ao menos uma missão exclusiva Premium sem gasto.");
  if(t.getFimEm().isBefore(LocalDate.now()))erros.add("A temporada já terminou.");
  var receita=(t.getPreco()==null?BigDecimal.ZERO:t.getPreco()).multiply(BigDecimal.valueOf(t.getPrevisaoPremium()));
  return new Previa(linhas,erros,total,receita,receita.subtract(total));
 }
 public void publicar(Long id,String autor){
  config.bloquearPublicacao().orElseThrow(()->new IllegalStateException("Abra a configuração de fidelidade antes de publicar."));
  var t=rascunho(id);if(temporadas.findAll().stream().anyMatch(o->!o.getId().equals(id)&&"PUBLICADO".equals(o.getEstado())&&!o.getFimEm().isBefore(t.getInicioEm())&&!o.getInicioEm().isAfter(t.getFimEm())))throw new IllegalStateException("Existe uma temporada publicada no mesmo período.");
  var p=previa(id);if(!p.pendencias().isEmpty())throw new IllegalStateException(String.join("; ",p.pendencias()));t.setEstado("PUBLICADO");t.setAtiva(true);t.setPublicadoPor(autor);t.setPublicadoEm(LocalDateTime.now());
 }
 public void encerrar(Long id){var t=temporadas.bloquear(id).orElseThrow();if(!"PUBLICADO".equals(t.getEstado())||!LocalDate.now().isAfter(t.getFimEm()))throw new IllegalStateException("O encerramento só é permitido após o último dia, preservando o prazo oferecido aos clientes.");t.setEstado("ENCERRADO");t.setAtiva(false);}
}
