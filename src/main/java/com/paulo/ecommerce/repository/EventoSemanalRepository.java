package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.EventoSemanal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoSemanalRepository extends JpaRepository<EventoSemanal, Long> {
  List<EventoSemanal> findTop30ByOrderByInicioDesc();
  long countByInicioLessThanEqualAndFimGreaterThan(LocalDateTime inicio, LocalDateTime fim);
  List<EventoSemanal> findByInicioLessThanEqualAndFimGreaterThan(LocalDateTime inicio, LocalDateTime fim);
}
