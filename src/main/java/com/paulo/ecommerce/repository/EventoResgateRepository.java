package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.EventoResgate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoResgateRepository extends JpaRepository<EventoResgate, Long> {
  boolean existsByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);
  List<EventoResgate> findByUsuarioIdOrderByResgatadoEmDesc(Long usuarioId);
}
