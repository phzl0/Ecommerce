package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.PasseOrigemXpTipo;
import com.paulo.ecommerce.model.PasseXpHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasseXpHistoricoRepository extends JpaRepository<PasseXpHistorico, Long> {
    Optional<PasseXpHistorico> findByUsuarioIdAndTemporadaIdAndOrigemTipoAndReferencia(Long usuarioId, Long temporadaId, PasseOrigemXpTipo origemTipo, String referencia);
    List<PasseXpHistorico> findTop10ByUsuarioIdAndTemporadaIdOrderByCriadoEmDesc(Long usuarioId, Long temporadaId);
}
