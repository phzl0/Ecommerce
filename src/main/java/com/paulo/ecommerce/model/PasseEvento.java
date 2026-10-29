package com.paulo.ecommerce.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "passe_eventos",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"usuario_id", "dia", "acao", "referencia"}))
public class PasseEvento {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "usuario_id", nullable = false)
  public Long usuarioId;

  @Column(nullable = false)
  public LocalDate dia;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public PasseAcaoTipo acao;

  @Column(nullable = false, length = 100)
  public String referencia;
}
