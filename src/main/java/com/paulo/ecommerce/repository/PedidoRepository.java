package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
  java.util.Optional<Pedido> findByUsuarioIdAndCheckoutKey(Long usuarioId, String checkoutKey);

  java.util.Optional<Pedido> findFirstByUsuarioIdAndPasseMensalTrueAndStatusAndDataPedidoAfterOrderByIdDesc(
      Long usuarioId, Pedido.Status status, LocalDateTime limite);

  @Query("""
      select p from Pedido p join fetch p.pagamento pg
      where p.usuario.id = :usuarioId and p.temporadaPasseId is not null
        and p.status <> 'CANCELADO' and pg.status = 'APROVADO'
        and (p.temporadaPasseId = :temporadaId or (p.passeMensal = true and pg.pagoEm >= :limite))
      """)
  List<Pedido> pagamentosPasse(Long usuarioId, Long temporadaId, LocalDateTime limite);

  List<Pedido> findTop100ByStatusAndDataPedidoBeforeOrderByIdAsc(
      Pedido.Status status, LocalDateTime limite);

  @Query("""
      select p.subtotal - p.desconto from Pedido p join p.pagamento pg
      where p.usuario.id = :usuario and p.temporadaPasseId is null
        and p.status <> 'CANCELADO' and pg.status = 'APROVADO'
        and pg.pagoEm >= :inicio and pg.pagoEm < :fim
        and p.subtotal - p.desconto >= :minimo
      """)
  List<BigDecimal> comprasEvento(Long usuario, LocalDateTime inicio, LocalDateTime fim, BigDecimal minimo);

  @Query("select p.usuario.email from Pedido p where p.id = :id")
  java.util.Optional<String> findOwnerEmail(@Param("id") Long id);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select x from Pedido x where x.id = :value")
  java.util.Optional<Pedido> findLockedById(
      @org.springframework.data.repository.query.Param("value") Long value);

  @EntityGraph(attributePaths = {"usuario", "itens", "itens.produto"})
  List<Pedido> findByUsuarioIdOrderByDataPedidoDesc(Long usuarioId);

  @EntityGraph(attributePaths = {"usuario", "itens", "itens.produto"})
  List<Pedido> findAll(Sort sort);

  @Query(
      """
        SELECT COALESCE(SUM(p.total), 0)
        FROM Pedido p
        WHERE p.status != 'CANCELADO' AND p.pagamento.status = 'APROVADO'
        AND p.dataPedido >= :inicio
        """)
  BigDecimal receitaDesde(@Param("inicio") LocalDateTime inicio);

  @Query(
      """
        SELECT p FROM Pedido p
        LEFT JOIN FETCH p.usuario
        LEFT JOIN FETCH p.itens i
        LEFT JOIN FETCH i.produto
        WHERE p.id = :id
        """)
  java.util.Optional<Pedido> findByIdComDetalhes(@Param("id") Long id);

  @Query("SELECT COUNT(p) FROM Pedido p WHERE p.status = :status")
  long countByStatus(@Param("status") Pedido.Status status);

  @Query(
      """
        SELECT p FROM Pedido p
        LEFT JOIN FETCH p.usuario
        WHERE p.dataPedido >= :inicio
        ORDER BY p.dataPedido DESC
        """)
  List<Pedido> findPedidosRecentes(@Param("inicio") LocalDateTime inicio);
}
