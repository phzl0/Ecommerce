package com.paulo.ecommerce.service;

import com.paulo.ecommerce.exception.RecursoNaoEncontradoException;
import com.paulo.ecommerce.model.Produto;
import com.paulo.ecommerce.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

  @org.springframework.beans.factory.annotation.Autowired private EstoqueService estoque;
  private final ProdutoRepository produtoRepository;

  public ProdutoService(ProdutoRepository produtoRepository) {
    this.produtoRepository = produtoRepository;
  }

  @Cacheable(value = "produtos", key = "#pagina + '-' + #tamanho")
  public Page<Produto> listarAtivos(int pagina, int tamanho) {
    Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("nome").ascending());
    return produtoRepository.findByAtivoTrue(pageable);
  }

  public List<Produto> buscarPorNome(String nome) {
    return produtoRepository.findByNomeContainingIgnoreCaseAndAtivoTrue(nome);
  }

  public List<Produto> listarDestaques(int limite) {
    return produtoRepository.findByAtivoTrueAndDestaqueTrueOrderByAtualizadoEmDesc(
        PageRequest.of(0, limite));
  }

  public List<Produto> listarRecentes(int limite) {
    return produtoRepository.findMaisRecentes(PageRequest.of(0, limite));
  }

  @Cacheable(value = "produtos", key = "'categoria-' + #categoriaId")
  public List<Produto> listarPorCategoria(Long categoriaId) {
    return produtoRepository.findByCategoriaIdAndAtivoTrue(categoriaId);
  }

  @Cacheable(value = "produto", key = "#id")
  public Produto buscarPorId(Long id) {
    return produtoRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
  }

  public List<Produto> filtrarCatalogo(
      String busca, Long categoriaId, BigDecimal precoMin, BigDecimal precoMax, String ordenacao) {
    List<Produto> produtos;

    if (busca != null && !busca.isBlank()) {
      produtos = buscarPorNome(busca);
    } else if (categoriaId != null) {
      produtos = listarPorCategoria(categoriaId);
    } else {
      produtos = listarAtivos(0, 200).getContent();
    }

    return produtos.stream()
        .filter(
            produto ->
                categoriaId == null
                    || (produto.getCategoria() != null
                        && categoriaId.equals(produto.getCategoria().getId())))
        .filter(produto -> precoMin == null || produto.getPreco().compareTo(precoMin) >= 0)
        .filter(produto -> precoMax == null || produto.getPreco().compareTo(precoMax) <= 0)
        .sorted(obterComparador(ordenacao))
        .toList();
  }

  @Transactional
  @CacheEvict(
      value = {"produto", "produtos"},
      allEntries = true)
  public Produto salvar(Produto produto) {
    if (produto.getSku() == null || produto.getSku().isBlank()) {
      produto.setSku(gerarSku(produto));
    } else {
      produto.setSku(produto.getSku().trim().toUpperCase(Locale.ROOT));
    }
    return produtoRepository.save(produto);
  }

  @Transactional
  @CacheEvict(value = {"produto", "produtos"}, allEntries = true)
  public Produto salvarCadastro(Produto cadastro, Integer estoqueOriginal) {
    validarSkuComercial(cadastro);
    validarCatalogo(cadastro);
    if (cadastro.getId() == null) {
      cadastro.setAtivo(cadastro.getPublicacao()==Produto.Publicacao.PUBLICADO);
      var novo=salvar(cadastro); estoque.registrar(novo,novo.getEstoque(),"ENTRADA","Cadastro inicial","administrador");return novo;
    }

    Produto produto = produtoComercialBloqueado(cadastro.getId());
    if (estoqueOriginal==null || !estoqueOriginal.equals(cadastro.getEstoque()))
      throw new IllegalStateException("Use a tela de estoque para ajustar o saldo com um motivo.");
    if(produto.getTipo()!=cadastro.getTipo())
      throw new IllegalArgumentException("Crie um novo SKU para mudar o tipo de produto e preservar os pedidos existentes.");
    if(!java.util.Objects.equals(produto.getProdutoPrincipalId(),cadastro.getProdutoPrincipalId()))throw new IllegalArgumentException("O vínculo da variação não pode ser alterado após o cadastro.");
    copiarCatalogo(cadastro,produto);
    produto.setNome(cadastro.getNome());
    produto.setSku(cadastro.getSku());
    produto.setMarca(cadastro.getMarca());
    produto.setTipo(cadastro.getTipo());
    produto.setCategoria(cadastro.getCategoria());
    produto.setPreco(cadastro.getPreco());
    produto.setDescricaoCurta(cadastro.getDescricaoCurta());
    produto.setDescricao(cadastro.getDescricao());
    produto.setDestaque(cadastro.isDestaque());
    if (cadastro.getImagemUrl() != null) produto.setImagemUrl(cadastro.getImagemUrl());
    if (produto.getProdutoPrincipalId() == null) {
      for (var variacao : produtoRepository.findByProdutoPrincipalIdOrderByIdAsc(produto.getId())) {
        variacao.setNome(produto.getNome()); variacao.setCategoria(produto.getCategoria());
      }
    }
    return salvar(produto);
  }

  @Transactional
  @CacheEvict(value = {"produto", "produtos"}, allEntries = true)
  public void definirVisibilidade(Long id, boolean ativo) {
    var p=produtoComercialBloqueado(id);
    p.setPublicacao(ativo?Produto.Publicacao.PUBLICADO:Produto.Publicacao.DESATIVADO);
    validarCatalogo(p);p.setAtivo(ativo);
  }

  public boolean disponivelNaLoja(Produto p){
    return p.publicado()&&(p.getProdutoPrincipalId()==null||produtoRepository.findById(p.getProdutoPrincipalId()).map(Produto::publicado).orElse(false));
  }
  public List<Produto> familia(Produto p){return produtoRepository.familia(p.getProdutoPrincipalId()==null?p.getId():p.getProdutoPrincipalId());}
  private void validarCatalogo(Produto p){
    if(p.getPublicacao()==null)throw new IllegalArgumentException("Escolha o estado de publicação.");
    if(p.getNome()==null||p.getNome().isBlank()||p.getNome().length()>255||p.getPreco()==null||p.getPreco().signum()<=0||p.getPreco().scale()>2)throw new IllegalArgumentException("Confira nome e preço do produto.");
    p.setCor(limpar(p.getCor()));p.setTamanho(limpar(p.getTamanho()));p.setModelo(limpar(p.getModelo()));
    if(p.getProdutoPrincipalId()!=null){
      var raiz=produtoRepository.findLockedById(p.getProdutoPrincipalId()).orElseThrow(()->new IllegalArgumentException("Produto principal inexistente."));
      if(raiz.getProdutoPrincipalId()!=null||java.util.Objects.equals(raiz.getId(),p.getId())||raiz.getTipo()!=p.getTipo())throw new IllegalArgumentException("Escolha um produto principal do mesmo tipo; não é permitido encadear variações.");
      validarSkuComercial(raiz);
      if(p.opcao().isBlank())throw new IllegalArgumentException("Informe tamanho, cor ou modelo para identificar a variação.");
      p.setNome(raiz.getNome());p.setCategoria(raiz.getCategoria());
      if(produtoRepository.familia(raiz.getId()).stream().anyMatch(v->!java.util.Objects.equals(v.getId(),p.getId())&&v.opcao().equalsIgnoreCase(p.opcao())))throw new IllegalArgumentException("Já existe uma variação com essa combinação.");
    }else if(p.getId()!=null && !p.opcao().isBlank() && produtoRepository.findByProdutoPrincipalIdOrderByIdAsc(p.getId()).stream().anyMatch(v->v.opcao().equalsIgnoreCase(p.opcao())))throw new IllegalArgumentException("Combinação já usada por uma variação.");
    if(p.getXpExtra()<0||p.getXpExtra()>10000||p.getXpExtra()>0&&(p.getXpExtraInicio()==null||p.getXpExtraFim()==null||!p.getXpExtraFim().isAfter(p.getXpExtraInicio())))throw new IllegalArgumentException("XP extra exige um período válido e valor de 1 a 10.000 por unidade.");
    if(p.getTipo()==Produto.Tipo.DIGITAL){p.setPesoKg(null);p.setComprimentoCm(null);p.setLarguraCm(null);p.setAlturaCm(null);}
  }
  private String limpar(String v){return v==null||v.isBlank()?null:v.strip();}
  private void copiarCatalogo(Produto a,Produto b){
    b.setPublicacao(a.getPublicacao());b.setAtivo(a.getPublicacao()==Produto.Publicacao.PUBLICADO);
    b.setCor(a.getCor());b.setTamanho(a.getTamanho());b.setModelo(a.getModelo());
    b.setPesoKg(a.getPesoKg());b.setComprimentoCm(a.getComprimentoCm());b.setLarguraCm(a.getLarguraCm());b.setAlturaCm(a.getAlturaCm());
    b.setXpExtra(a.getXpExtra());b.setXpExtraInicio(a.getXpExtraInicio());b.setXpExtraFim(a.getXpExtraFim());
  }
  @Transactional @CacheEvict(value={"produto","produtos"},allEntries=true)
  public void galeria(Long id,List<String> novas,Integer indice,String acao){
    var p=produtoComercialBloqueado(id);var imagens=new java.util.ArrayList<>(p.imagens());
    if(novas!=null){if(imagens.size()+novas.size()>8)throw new IllegalArgumentException("Use no máximo oito imagens por SKU.");imagens.addAll(novas);}
    if(indice!=null){if(indice<0||indice>=imagens.size())throw new IllegalArgumentException("Imagem inexistente.");
      switch(acao){case "remover"->imagens.remove((int)indice);case "capa"->{var img=imagens.remove((int)indice);imagens.add(0,img);}case "subir"->{if(indice>0)java.util.Collections.swap(imagens,indice,indice-1);}case "descer"->{if(indice<imagens.size()-1)java.util.Collections.swap(imagens,indice,indice+1);}default->throw new IllegalArgumentException("Ação inválida.");}}
    p.setImagemUrl(imagens.isEmpty()?null:imagens.remove(0));p.setGaleria(String.join("\n",imagens));
  }

  private Produto produtoComercialBloqueado(Long id) {
    Produto produto = produtoRepository.findLockedById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    validarSkuComercial(produto);
    return produto;
  }

  private void validarSkuComercial(Produto produto) {
    if (produto.getSku() != null && produto.getSku().strip().toUpperCase(Locale.ROOT).startsWith("PASSE-MENSAL-")) {
      throw new IllegalArgumentException("A família de SKUs PASSE-MENSAL é reservada à assinatura.");
    }
  }

  @Transactional
  @CacheEvict(
      value = {"produto", "produtos"},
      allEntries = true)
  public void deletar(Long id) {
    Produto produto =
        produtoRepository.findLockedById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    produto.setAtivo(false);
    produtoRepository.save(produto);
  }

  private Comparator<Produto> obterComparador(String ordenacao) {
    if (ordenacao == null || ordenacao.isBlank()) {
      return Comparator.comparing(Produto::isDestaque)
          .reversed()
          .thenComparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER);
    }

    return switch (ordenacao) {
      case "preco-asc" -> Comparator.comparing(Produto::getPreco)
          .thenComparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER);
      case "preco-desc" -> Comparator.comparing(Produto::getPreco)
          .reversed()
          .thenComparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER);
      case "recentes" -> Comparator.comparing(Produto::getCriadoEm).reversed();
      case "nome-desc" -> Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER)
          .reversed();
      default -> Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER);
    };
  }

  private String gerarSku(Produto produto) {
    String base =
        produto.getNome() == null
            ? "PRD"
            : produto.getNome().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    if (base.isBlank()) {
      base = "PRD";
    }
    base = base.substring(0, Math.min(base.length(), 8));
    return base + "-" + java.util.UUID.randomUUID().toString().substring(0,12);
  }
}
