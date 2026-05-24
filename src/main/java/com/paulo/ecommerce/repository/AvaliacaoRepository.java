package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByProdutoIdOrderByCriadoEmDesc(Long produtoId);

    Optional<Avaliacao> findByUsuarioIdAndProdutoId(Long usuarioId, Long produtoId);

    boolean existsByUsuarioIdAndProdutoId(Long usuarioId, Long produtoId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.produto.id = :produtoId")
    Double mediaNotasPorProduto(Long produtoId);

    long countByProdutoId(Long produtoId);
}