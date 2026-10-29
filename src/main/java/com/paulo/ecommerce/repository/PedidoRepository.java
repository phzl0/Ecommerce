package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.Pedido;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select x from Pedido x where x.id = :value")
    java.util.Optional<Pedido> findLockedById(@org.springframework.data.repository.query.Param("value") Long value);

    @EntityGraph(attributePaths = {"usuario", "itens", "itens.produto"})
    List<Pedido> findByUsuarioIdOrderByDataPedidoDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "itens", "itens.produto"})
    List<Pedido> findAll(Sort sort);

    @Query("""
        SELECT COALESCE(SUM(p.total), 0)
        FROM Pedido p
        WHERE p.status != 'CANCELADO'
        AND p.dataPedido >= :inicio
        """)
    BigDecimal receitaDesde(@Param("inicio") LocalDateTime inicio);

    @Query("""
        SELECT p FROM Pedido p
        LEFT JOIN FETCH p.usuario
        LEFT JOIN FETCH p.itens i
        LEFT JOIN FETCH i.produto
        WHERE p.id = :id
        """)
    java.util.Optional<Pedido> findByIdComDetalhes(@Param("id") Long id);

    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.status = :status")
    long countByStatus(
            @Param("status") Pedido.Status status);

    @Query("""
        SELECT p FROM Pedido p
        LEFT JOIN FETCH p.usuario
        WHERE p.dataPedido >= :inicio
        ORDER BY p.dataPedido DESC
        """)
    List<Pedido> findPedidosRecentes(
            @Param("inicio") LocalDateTime inicio);
}
