package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.EstoqueReserva;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EstoqueReservaRepository extends JpaRepository<EstoqueReserva,Long> {
 java.util.Optional<EstoqueReserva> findByProdutoIdAndReferencia(Long produtoId, String referencia);
 java.util.List<EstoqueReserva> findByReferenciaOrderByProdutoIdAsc(String referencia);
 @org.springframework.data.jpa.repository.Query("select coalesce(sum(r.quantidade),0) from EstoqueReserva r where r.produtoId=:id and r.estado='RESERVADO' and r.referencia like :prefixo")
 long totalReservado(Long id,String prefixo);
}
