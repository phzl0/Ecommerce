package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.Produto;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select x from Produto x where x.id = :value")
    java.util.Optional<Produto> findLockedById(@org.springframework.data.repository.query.Param("value") Long value);

    boolean existsByNomeIgnoreCase(String nome);

    @EntityGraph(attributePaths = {"categoria"})
    Page<Produto> findByAtivoTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"categoria"})
    List<Produto> findByNomeContainingIgnoreCaseAndAtivoTrue(String nome);

    @EntityGraph(attributePaths = {"categoria"})
    List<Produto> findByCategoriaIdAndAtivoTrue(Long categoriaId);

    @EntityGraph(attributePaths = {"categoria"})
    Optional<Produto> findById(Long id);

    @Query("""
        SELECT p FROM Produto p
        LEFT JOIN FETCH p.categoria
        WHERE p.ativo = true
        AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId)
        AND (:busca IS NULL OR LOWER(p.nome) LIKE LOWER(CONCAT('%',:busca,'%')))
        ORDER BY p.nome
        """)
    List<Produto> buscarComFiltros(
            @Param("categoriaId") Long categoriaId,
            @Param("busca") String busca);

    @Query("SELECT COUNT(p) FROM Produto p WHERE p.estoque = 0 AND p.ativo = true")
    long countSemEstoque();

    @Query("""
        SELECT p FROM Produto p
        WHERE p.ativo = true
        AND p.estoque > 0
        ORDER BY p.criadoEm DESC
        """)
    List<Produto> findMaisRecentes(Pageable pageable);

    @EntityGraph(attributePaths = {"categoria"})
    List<Produto> findByAtivoTrueAndDestaqueTrueOrderByAtualizadoEmDesc(Pageable pageable);
}
