package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.PasseOrigemXpTipo;
import com.paulo.ecommerce.model.PasseXpHistorico;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasseXpHistoricoRepository extends JpaRepository<PasseXpHistorico, Long> {
  List<PasseXpHistorico> findByUsuarioIdAndTemporadaId(Long usuarioId, Long temporadaId);

  List<PasseXpHistorico> findByUsuarioIdAndOrigemTipoAndReferencia(
      Long usuarioId, PasseOrigemXpTipo origemTipo, String referencia);

  Optional<PasseXpHistorico> findByUsuarioIdAndTemporadaIdAndOrigemTipoAndReferencia(
      Long usuarioId, Long temporadaId, PasseOrigemXpTipo origemTipo, String referencia);

  List<PasseXpHistorico> findTop10ByUsuarioIdAndTemporadaIdOrderByCriadoEmDesc(
      Long usuarioId, Long temporadaId);
}
