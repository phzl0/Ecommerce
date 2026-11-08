package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.Notificacao;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
  List<Notificacao> findTop30ByUsuarioIdOrderByIdDesc(Long usuarioId);
  long countByUsuarioIdAndLidaFalse(Long usuarioId);

  @Modifying
  @Query("update Notificacao n set n.lida = true where n.usuarioId = :usuarioId and n.lida = false and n.id <= :ate")
  void marcarLidas(Long usuarioId, Long ate);
}
