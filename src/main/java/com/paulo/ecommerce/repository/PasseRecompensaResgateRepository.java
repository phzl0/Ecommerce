package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.PasseRecompensaResgate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasseRecompensaResgateRepository
    extends JpaRepository<PasseRecompensaResgate, Long> {
  Optional<PasseRecompensaResgate> findByUsuarioIdAndCodigoEntrega(
      Long usuarioId, String codigoEntrega);

  List<PasseRecompensaResgate> findByUsuarioId(Long usuarioId);

  Optional<PasseRecompensaResgate> findByUsuarioIdAndRecompensaId(
      Long usuarioId, Long recompensaId);

  List<PasseRecompensaResgate> findByUsuarioIdAndTemporadaId(Long usuarioId, Long temporadaId);
}
