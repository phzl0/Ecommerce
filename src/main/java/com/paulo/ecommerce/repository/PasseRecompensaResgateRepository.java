package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.PasseRecompensaResgate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasseRecompensaResgateRepository extends JpaRepository<PasseRecompensaResgate, Long> {
    Optional<PasseRecompensaResgate> findByUsuarioIdAndRecompensaId(Long usuarioId, Long recompensaId);
    List<PasseRecompensaResgate> findByUsuarioIdAndTemporadaId(Long usuarioId, Long temporadaId);
}
