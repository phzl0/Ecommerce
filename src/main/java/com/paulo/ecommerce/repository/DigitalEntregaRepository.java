package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.DigitalEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DigitalEntregaRepository extends JpaRepository<DigitalEntrega,Long> {
 java.util.List<DigitalEntrega> findByPedidoIdOrderByIdAsc(Long pedidoId);
 java.util.List<DigitalEntrega> findByUsuarioIdOrderByIdDesc(Long usuarioId);
 java.util.List<DigitalEntrega> findTop100ByOrderByIdDesc();
 boolean existsByItemId(Long itemId);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select e from DigitalEntrega e where e.id=:id")
 java.util.Optional<DigitalEntrega> findLockedById(Long id);
}
