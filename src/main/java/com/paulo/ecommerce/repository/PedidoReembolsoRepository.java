package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.PedidoReembolso;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
public interface PedidoReembolsoRepository extends JpaRepository<PedidoReembolso,Long> {
 List<PedidoReembolso> findByPedidoIdOrderByIdDesc(Long pedidoId);
 Optional<PedidoReembolso> findByPedidoIdAndChave(Long pedidoId,String chave);
 @Query("select r from PedidoReembolso r where (:estado='' or r.estado=:estado) and (:analise=false or (r.analiseBeneficios=true and r.analisadoEm is null)) order by r.id desc")
 Page<PedidoReembolso> fila(String estado,boolean analise,Pageable page);
}
