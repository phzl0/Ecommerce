package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.util.*;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;

@Service
@Transactional
public class DigitalService {
  private final DigitalConfigRepository configs;
  private final DigitalArquivoRepository arquivos;
  private final DigitalCodigoRepository codigos;
  private final DigitalEntregaRepository entregas;
  private final ProdutoRepository produtos;
  private final UsuarioRepository usuarios;
  private final PedidoRepository pedidos;
  private final EstoqueService estoque;
  public DigitalService(DigitalConfigRepository c, DigitalArquivoRepository a, DigitalCodigoRepository k,
      DigitalEntregaRepository e, ProdutoRepository p, UsuarioRepository u, PedidoRepository pedidos, EstoqueService estoque) {
    configs=c;arquivos=a;codigos=k;entregas=e;produtos=p;usuarios=u;this.pedidos=pedidos;this.estoque=estoque;
  }
  private Produto produtoDigital(Long id) {
    var p=produtos.findLockedById(id).orElseThrow();
    if(p.getTipo()!=Produto.Tipo.DIGITAL || (p.getSku()!=null && p.getSku().startsWith("PASSE-MENSAL-")))
      throw new IllegalArgumentException("Escolha um produto digital comercial.");
    return p;
  }
  private DigitalConfig config(Long id,String modo) {
    var c=configs.findById(id).orElseGet(DigitalConfig::new);
    if(c.modo!=null && !modo.equals(c.modo)) throw new IllegalArgumentException("Crie outro SKU para mudar o formato de entrega.");
    c.produtoId=id;c.modo=modo;return c;
  }
  public void arquivo(Long id,String nome,byte[] dados) {
    produtoDigital(id);
    if(nome==null || !nome.toLowerCase(Locale.ROOT).matches(".*\\.(pdf|txt|zip)$") || dados.length==0 || dados.length>5*1024*1024)
      throw new IllegalArgumentException("Envie PDF, TXT ou ZIP de até 5 MB.");
    var cfg=config(id,"ARQUIVO");
    var a=new DigitalArquivo();a.produtoId=id;a.nome=nome.replaceAll("[^a-zA-Z0-9._-]","_");
    if(a.nome.length()>160) throw new IllegalArgumentException("Nome de arquivo muito longo.");
    a.dados=dados;arquivos.save(a);cfg.arquivoId=a.id;configs.save(cfg);
  }
  @CacheEvict(value={"produto","produtos","metricas"},allEntries=true)
  public int importarCodigos(Long id,String texto,String autor) {
    var p=produtoDigital(id);var cfg=config(id,"CODIGO");
    if(texto==null || texto.length()>250000) throw new IllegalArgumentException("Importe até 500 códigos por vez.");
    var linhas=texto.lines().map(String::strip).filter(s->!s.isBlank()).toList();
    if(linhas.isEmpty() || linhas.size()>500 || linhas.stream().anyMatch(s->s.length()>500))
      throw new IllegalArgumentException("Informe de 1 a 500 códigos, com até 500 caracteres cada.");
    if(cfg.produtoId!=null && !configs.existsById(id) && p.getEstoque()!=0)
      throw new IllegalArgumentException("Zere o saldo demonstrativo no estoque antes de iniciar o controle por códigos.");
    int total=0;
    for(var valor:linhas) {
      String hash;
      try { hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); }
      catch(java.security.NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
      if(codigos.existsByProdutoIdAndHash(id,hash)) continue;
      var c=new DigitalCodigo();c.produtoId=id;c.valor=valor;c.hash=hash;codigos.saveAndFlush(c);total++;
    }
    p.setEstoque(Math.addExact(p.getEstoque(),total)); configs.save(cfg);
    estoque.registrar(p,total,"ENTRADA","Importação de códigos digitais",autor);return total;
  }
  // Caller owns the order/customer lock and product locks in ascending order.
  public void reservar(Pedido pedido) {
    if(pedido.getTemporadaPasseId()!=null || pedido.isRecompensa()) return;
    for(var item:pedido.getItens().stream().sorted(Comparator.comparing(i->i.getProduto().getId())).toList()) {
      if(item.getTipo()!=Produto.Tipo.DIGITAL || entregas.existsByItemId(item.getId())) continue;
      var cfg=configs.findById(item.getProduto().getId()).orElseThrow(()->new IllegalStateException("Conteúdo digital ainda não configurado: "+item.getProduto().getNome()));
      var livres="CODIGO".equals(cfg.modo)?codigos.findByProdutoIdAndEstadoOrderByIdAsc(cfg.produtoId,"DISPONIVEL",org.springframework.data.domain.PageRequest.of(0,item.getQuantidade())):List.<DigitalCodigo>of();
      if("CODIGO".equals(cfg.modo) && livres.size()!=item.getQuantidade()) throw new IllegalStateException("Códigos insuficientes para este pedido.");
      if("ARQUIVO".equals(cfg.modo) && cfg.arquivoId==null) throw new IllegalStateException("Arquivo indisponível.");
      for(int n=0;n<item.getQuantidade();n++) {
        var e=new DigitalEntrega();e.usuarioId=pedido.getUsuario().getId();e.pedidoId=pedido.getId();e.produtoId=cfg.produtoId;
        e.itemId=item.getId();e.unidade=n+1;e.titulo=item.getProduto().getNome();e.modo=cfg.modo;e.arquivoId=cfg.arquivoId;
        if(!livres.isEmpty()) {var c=livres.get(n);c.estado="RESERVADO";e.codigoId=c.id;}
        entregas.save(e);
      }
    }
  }
  public void liberar(Pedido pedido) {
    if(pedido.getPagamento()==null || pedido.getPagamento().getStatus()!=Pagamento.StatusPagamento.APROVADO)
      throw new IllegalStateException("Entrega digital exige pagamento aprovado.");
    for(var e:entregas.findByPedidoIdOrderByIdAsc(pedido.getId())) {
      if(!"PENDENTE".equals(e.estado)) continue;
      e.estado="LIBERADO";e.liberadoEm=LocalDateTime.now();
      if(e.codigoId!=null) codigos.findById(e.codigoId).orElseThrow().estado="ENTREGUE";
    }
    estoque.consumir("PEDIDO:"+pedido.getId(),true);
  }
  public void revogar(Pedido pedido) {
    for(var e:entregas.findByPedidoIdOrderByIdAsc(pedido.getId())) {
      // Never recycle a code that was made accessible after approval, even if not viewed.
      if(e.codigoId!=null) {
        var c=codigos.findById(e.codigoId).orElseThrow();
        if("PENDENTE".equals(e.estado)) {c.estado="DISPONIVEL";e.codigoId=null;}
        else c.estado="INUTILIZADO";
      }
      e.estado="REVOGADO";
    }
  }
  public record EntregaView(Long id,Long pedidoId,String titulo,String modo,String estado,LocalDateTime liberadoEm,LocalDateTime acessadoEm) {}
  @Transactional(readOnly=true)
  public List<EntregaView> listar(String email) {
    var u=usuarios.findByEmail(email).orElseThrow();
    return entregas.findByUsuarioIdOrderByIdDesc(u.getId()).stream()
        .map(e->new EntregaView(e.id,e.pedidoId,e.titulo,e.modo,e.estado,e.liberadoEm,e.acessadoEm)).toList();
  }
  private DigitalEntrega autorizar(String email,Long id) {
    var e=entregas.findById(id).orElseThrow();
    var u=usuarios.findByEmail(email).orElseThrow();
    if(!u.getId().equals(e.usuarioId)) throw new AccessDeniedException("Conteúdo pertence a outra conta.");
    // Same lock as cancellation, so delivery and revocation cannot race.
    var p=pedidos.findLockedById(e.pedidoId).orElseThrow();
    e=entregas.findLockedById(id).orElseThrow();
    if(!"LIBERADO".equals(e.estado) || p.getStatus()==Pedido.Status.CANCELADO || p.getPagamento()==null
        || p.getPagamento().getStatus()!=Pagamento.StatusPagamento.APROVADO) throw new AccessDeniedException("Conteúdo não disponível para este pedido.");
    e.acessadoEm=LocalDateTime.now();return e;
  }
  public DigitalArquivo baixar(String email,Long id) {
    var e=autorizar(email,id);
    if(!"ARQUIVO".equals(e.modo)) throw new IllegalArgumentException("Este produto é entregue por código.");
    return arquivos.findById(e.arquivoId).orElseThrow();
  }
  public String revelar(String email,Long id) {
    var e=autorizar(email,id);
    if(!"CODIGO".equals(e.modo)) throw new IllegalArgumentException("Este produto é entregue por arquivo.");
    return codigos.findById(e.codigoId).orElseThrow().valor;
  }
}
