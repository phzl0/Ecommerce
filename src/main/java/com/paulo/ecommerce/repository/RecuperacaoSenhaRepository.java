package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.RecuperacaoSenha;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {
  @Query("select r.usuarioId from RecuperacaoSenha r where r.hash = :hash")
  Optional<Long> identificarUsuario(String hash);
}
