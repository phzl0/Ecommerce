package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.PasseMissaoProgresso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasseMissaoProgressoRepository extends JpaRepository<PasseMissaoProgresso, Long> {
    Optional<PasseMissaoProgresso> findByUsuarioIdAndTemplateIdAndPeriodoInicio(Long usuarioId, Long templateId, LocalDateTime periodoInicio);
    List<PasseMissaoProgresso> findByUsuarioIdAndTemporadaId(Long usuarioId, Long temporadaId);
}
