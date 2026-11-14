package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.RecompensaOpcao;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RecompensaOpcaoRepository extends JpaRepository<RecompensaOpcao, Long> {
  java.util.List<RecompensaOpcao> findByRecompensaIdOrderByIdAsc(Long recompensaId);
}
