package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    Optional<Pagamento> findByPedidoId(Long pedidoId);
    Optional<Pagamento> findByMpPaymentId(String mpPaymentId);
    Optional<Pagamento> findByMpPreferenceId(String mpPreferenceId);
}