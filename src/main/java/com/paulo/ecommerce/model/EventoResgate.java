package com.paulo.ecommerce.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evento_resgates",
    uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "usuario_id"}))
public class EventoResgate {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false) public Long eventoId;
  @Column(nullable = false) public Long usuarioId;
  @Column(nullable = false) public LocalDateTime resgatadoEm;
}
