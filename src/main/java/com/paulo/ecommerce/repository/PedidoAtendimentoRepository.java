package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.PedidoAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PedidoAtendimentoRepository extends JpaRepository<PedidoAtendimento,Long> {
java.util.List<PedidoAtendimento> findTop50ByClienteTrueOrderByIdDesc();
 java.util.Optional<PedidoAtendimento> findByPedidoIdAndChave(Long pedidoId,String chave);
 java.util.List<PedidoAtendimento> findTop100ByPedidoIdOrderByIdDesc(Long pedidoId);
 java.util.List<PedidoAtendimento> findTop100ByPedidoIdAndPublicoTrueOrderByIdDesc(Long pedidoId);
}
