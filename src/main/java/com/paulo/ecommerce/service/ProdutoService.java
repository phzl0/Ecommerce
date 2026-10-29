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
  @CacheEvict(
      value = {"produto", "produtos"},
      allEntries = true)
  public void deletar(Long id) {
    Produto produto = buscarPorId(id);
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
    String sufixo = Long.toString(System.currentTimeMillis());
    return base + "-" + sufixo.substring(sufixo.length() - 6);
  }
}
