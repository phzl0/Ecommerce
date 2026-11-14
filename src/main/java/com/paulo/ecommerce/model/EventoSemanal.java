package com.paulo.ecommerce.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_semanais")
public class EventoSemanal {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 100) public String titulo;
  @Column(nullable = false, length = 500) public String descricao;
  @Column(nullable = false) public LocalDateTime inicio;
  @Column(nullable = false) public LocalDateTime fim;
  @Column(nullable = false) public int multiplicador;
  @Column(nullable = false) public int limiteXp;
  @Column(nullable = false) public int metaXp;
  @Column(nullable = false, precision = 10, scale = 2) public BigDecimal minimoCompra;
  @Column(nullable = false, length = 80) public String selo;
}
