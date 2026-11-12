package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

@Service
@Transactional
public class EstoqueService {
  private final ProdutoRepository produtos;
  private final EstoqueReservaRepository reservas;
  private final EstoqueMovimentoRepository movimentos;
  private final DigitalConfigRepository digitais;
  public EstoqueService(ProdutoRepository p, EstoqueReservaRepository r, EstoqueMovimentoRepository m, DigitalConfigRepository d) {
    produtos=p; reservas=r; movimentos=m; digitais=d;
  }
  public void registrar(Produto p, int delta, String tipo, String motivo, String autor) {
    var m=new EstoqueMovimento(); m.produtoId=p.getId(); m.quantidade=delta; m.saldo=p.getEstoque();
    m.tipo=tipo; m.motivo=motivo; m.autor=autor; movimentos.save(m);
  }
  @CacheEvict(value={"produto","produtos","metricas"},allEntries=true)
  public void ajustar(Long id,int delta,int minimo,String motivo,String autor) {
    if (motivo==null || motivo.strip().length()<5 || motivo.length()>200 || minimo<0 || minimo>1000000 || Math.abs((long)delta)>1000000)
      throw new IllegalArgumentException("Informe um motivo de 5 a 200 caracteres e quantidades válidas.");
    var p=produtos.findLockedById(id).orElseThrow();
    if (p.getSku()!=null && p.getSku().startsWith("PASSE-MENSAL-")) throw new IllegalArgumentException("Produto gerenciado pelo passe.");
    var cfg=digitais.findById(id);
    if (delta!=0 && cfg.isPresent() && "CODIGO".equals(cfg.get().modo))
      throw new IllegalArgumentException("O saldo de códigos é alterado pela importação e pelas vendas.");
    alterar(p,delta); p.setEstoqueMinimo(minimo); registrar(p,delta,"AJUSTE",motivo.strip(),autor);
  }
  public void reservar(Produto p,int quantidade,String ref) {
    p=produtos.findLockedById(p.getId()).orElseThrow();
    if (reservas.findByProdutoIdAndReferencia(p.getId(),ref).isPresent()) return;
    if (quantidade<1 || p.getEstoque()<quantidade) throw new IllegalStateException("Estoque insuficiente: "+p.getNome());
    alterar(p,-quantidade);
    var r=new EstoqueReserva(); r.produtoId=p.getId();r.quantidade=quantidade;r.referencia=ref;reservas.save(r);
    registrar(p,-quantidade,"RESERVA",ref,"sistema");
  }
  public void liberar(String ref) { transicao(ref,false,false); }
  public void consumir(String ref, boolean apenasDigital) { transicao(ref,true,apenasDigital); }
  private void transicao(String ref,boolean consumo,boolean apenasDigital) {
    for(var r:reservas.findByReferenciaOrderByProdutoIdAsc(ref)) {
      var p=produtos.findLockedById(r.produtoId).orElseThrow();
      if (!"RESERVADO".equals(r.estado) || (apenasDigital && p.getTipo()!=Produto.Tipo.DIGITAL)) continue;
      r.estado=consumo?"CONSUMIDO":"LIBERADO";
      if(!consumo) alterar(p,r.quantidade);
      registrar(p,consumo?0:r.quantidade,consumo?"SAIDA":"DEVOLUCAO",ref,"sistema");
    }
  }
  private void alterar(Produto p,int delta) {
    long saldo=(long)p.getEstoque()+delta;
    if(saldo<0 || saldo>Integer.MAX_VALUE) throw new IllegalArgumentException("Ajuste deixaria o estoque inválido.");
    p.setEstoque((int)saldo);
  }
}
