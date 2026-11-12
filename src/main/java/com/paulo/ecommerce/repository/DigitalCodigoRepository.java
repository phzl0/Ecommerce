package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.DigitalCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DigitalCodigoRepository extends JpaRepository<DigitalCodigo,Long> {
 java.util.List<DigitalCodigo> findByProdutoIdAndEstadoOrderByIdAsc(Long produtoId,String estado,org.springframework.data.domain.Pageable pagina);
 long countByProdutoIdAndEstado(Long produtoId,String estado);
 boolean existsByProdutoIdAndHash(Long produtoId,String hash);
}
