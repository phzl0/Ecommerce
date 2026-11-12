package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.Produto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select x from Produto x where x.id = :value")
  java.util.Optional<Produto> findLockedById(
      @org.springframework.data.repository.query.Param("value") Long value);

  List<Produto> findByTipoOrderByNomeAsc(Produto.Tipo tipo);
  Optional<Produto> findBySku(String sku);

  boolean existsByNomeIgnoreCase(String nome);

  @EntityGraph(attributePaths = {"categoria"})
  Page<Produto> findByAtivoTrue(Pageable pageable);

  @EntityGraph(attributePaths = {"categoria"})
  List<Produto> findByNomeContainingIgnoreCaseAndAtivoTrue(String nome);

  @EntityGraph(attributePaths = {"categoria"})
  List<Produto> findByCategoriaIdAndAtivoTrue(Long categoriaId);

  @EntityGraph(attributePaths = {"categoria"})
  Optional<Produto> findById(Long id);

  @Query(
      """
        SELECT p FROM Produto p
        LEFT JOIN FETCH p.categoria
        WHERE p.ativo = true
        AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId)
        AND (:busca IS NULL OR LOWER(p.nome) LIKE LOWER(CONCAT('%',:busca,'%')))
        ORDER BY p.nome
        """)
  List<Produto> buscarComFiltros(
      @Param("categoriaId") Long categoriaId, @Param("busca") String busca);

  @Query("SELECT COUNT(p) FROM Produto p WHERE p.estoque = 0 AND p.ativo = true")
  long countSemEstoque();

  long countByAtivoTrue();

  long countByAtivoTrueAndEstoqueBetween(int minimo, int maximo);
  @Query("select count(p) from Produto p where p.ativo=true and p.estoque <= p.estoqueMinimo")
  long countAbaixoMinimo();

  @Query("""
      select produto from Produto produto
      where produto.ativo = true and (:produtoId is null or produto.id = :produtoId)
        and exists (
          select 1 from Pedido pedido join pedido.itens item join pedido.pagamento pagamento
          where pedido.usuario.id = :usuarioId and item.produto.id = produto.id
            and pedido.status <> 'CANCELADO' and pagamento.status = 'APROVADO')
        and not exists (
          select 1 from Avaliacao avaliacao
          where avaliacao.usuario.id = :usuarioId and avaliacao.produto.id = produto.id)
      order by produto.id
      """)
  List<Produto> buscarParaAvaliar(Long usuarioId, Long produtoId, Pageable limite);

  @EntityGraph(attributePaths = {"categoria"})
  @Query("""
      select p from Produto p
      where (p.sku is null or lower(p.sku) not like 'passe-mensal-%')
      and (:ativo is null or p.ativo = :ativo)
      and (:semEstoque = false or p.estoque = 0)
      and (:estoqueBaixo = false or p.estoque between 1 and p.estoqueMinimo and :limiteEstoque >= 0)
      and (:busca = '' or locate(lower(:busca), lower(p.nome)) > 0
        or locate(lower(:busca), lower(coalesce(p.sku, ''))) > 0)
      """)
  Page<Produto> buscarCatalogoAdmin(String busca, Boolean ativo, boolean semEstoque,
      boolean estoqueBaixo, int limiteEstoque, Pageable pagina);

  @Query(
      """
        SELECT p FROM Produto p
        WHERE p.ativo = true
        AND p.estoque > 0
        ORDER BY p.criadoEm DESC
        """)
  List<Produto> findMaisRecentes(Pageable pageable);

  @EntityGraph(attributePaths = {"categoria"})
  List<Produto> findByAtivoTrueAndDestaqueTrueOrderByAtualizadoEmDesc(Pageable pageable);
}
