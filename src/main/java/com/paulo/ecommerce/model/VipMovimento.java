package com.paulo.ecommerce.model;

import jakarta.persistence.*;

@Entity
@Table(name="vip_movimentos", uniqueConstraints=@UniqueConstraint(columnNames={"usuarioId", "referencia"}))
public class VipMovimento {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long usuarioId;
  @Column(nullable=false, length=160) public String referencia;
  public Long pedidoId;
  public long pontos;
  @Column(nullable=false) public long pontosEstornados;
  public long liquido() { return estornado ? 0 : Math.max(0,pontos-pontosEstornados); }
  public boolean estornado;
  public java.time.LocalDateTime criadoEm = java.time.LocalDateTime.now();
}
