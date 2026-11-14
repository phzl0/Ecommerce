package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.BauItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BauItemRepository extends JpaRepository<BauItem, Long> {
  java.util.Optional<BauItem> findByPedidoId(Long pedidoId);
  java.util.List<BauItem> findTop100ByOrderByIdDesc();
  java.util.List<BauItem> findByUsuarioIdOrderByIdDesc(Long usuarioId);
  java.util.Optional<BauItem> findByUsuarioIdAndOrigem(Long usuarioId, String origem);
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select b from BauItem b where b.id = :id")
  java.util.Optional<BauItem> findLockedById(Long id);
}
