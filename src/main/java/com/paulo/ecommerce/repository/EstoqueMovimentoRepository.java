package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.EstoqueMovimento;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EstoqueMovimentoRepository extends JpaRepository<EstoqueMovimento,Long> {
 java.util.List<EstoqueMovimento> findTop100ByProdutoIdOrderByIdDesc(Long produtoId);
}
