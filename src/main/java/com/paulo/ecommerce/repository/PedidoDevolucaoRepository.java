package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.PedidoDevolucao;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PedidoDevolucaoRepository extends JpaRepository<PedidoDevolucao,Long> {
@org.springframework.data.jpa.repository.Query("select d from PedidoDevolucao d where (:estado='' or d.estado=:estado) order by d.id desc")
 org.springframework.data.domain.Page<PedidoDevolucao> fila(String estado,org.springframework.data.domain.Pageable pagina);
 java.util.Optional<PedidoDevolucao> findByPedidoIdAndChave(Long pedidoId,String chave);
 java.util.List<PedidoDevolucao> findByPedidoIdOrderByIdDesc(Long pedidoId);
 @org.springframework.data.jpa.repository.Query("select coalesce(sum(d.quantidade),0) from PedidoDevolucao d where d.itemId=:itemId and d.estado<>'RECUSADA'")
 long quantidadeSolicitada(Long itemId);
}
