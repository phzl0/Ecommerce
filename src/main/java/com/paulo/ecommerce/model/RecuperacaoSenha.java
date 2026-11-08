package com.paulo.ecommerce.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recuperacao_senha")
public class RecuperacaoSenha {
  @Id public Long usuarioId;

  @Column(nullable = false, unique = true, length = 64) public String hash;
  @Column(nullable = false) public LocalDateTime expiraEm;
}
