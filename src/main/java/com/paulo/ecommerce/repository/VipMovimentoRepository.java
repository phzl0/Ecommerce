package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.VipMovimento;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VipMovimentoRepository extends JpaRepository<VipMovimento, Long> {
  boolean existsByUsuarioIdAndReferencia(Long usuarioId, String referencia);
  java.util.List<VipMovimento> findByPedidoId(Long pedidoId);
  java.util.List<VipMovimento> findByUsuarioIdAndEstornadoFalseOrderByCriadoEmDesc(Long usuarioId);
}
