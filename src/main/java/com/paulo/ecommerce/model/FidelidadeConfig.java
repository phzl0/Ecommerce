package com.paulo.ecommerce.model;

import jakarta.persistence.*;

@Entity
@Table(name="fidelidade_config")
public class FidelidadeConfig {
  @Id public Long id = 1L;
  @Column(nullable=false, precision=10, scale=2) public java.math.BigDecimal precoPasse = new java.math.BigDecimal("79.00");
  public int diasInatividade = 90;
  public int pontosPorReal = 1;
  public int pontosMissao = 2;
  public int pontosEvento = 20;
  public int bonusPrimeiroPasse = 50;
  public int bonusPasseTemporada = 10;
}
