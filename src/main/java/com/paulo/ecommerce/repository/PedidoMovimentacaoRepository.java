package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.PedidoMovimentacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoMovimentacaoRepository extends JpaRepository<PedidoMovimentacao, Long> {
  List<PedidoMovimentacao> findByPedidoIdOrderByIdDesc(Long pedidoId);
}
