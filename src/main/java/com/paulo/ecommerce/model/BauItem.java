package com.paulo.ecommerce.model;

import jakarta.persistence.*;

@Entity
@Table(name="bau_itens", uniqueConstraints=@UniqueConstraint(columnNames={"usuarioId", "origem"}))
public class BauItem {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long usuarioId;
  @Column(nullable=false, length=100) public String origem;
  @Column(nullable=false, length=120) public String titulo;
  @ManyToOne public Produto produto;
  @ManyToOne public Pedido pedido;
  public java.time.LocalDateTime criadoEm = java.time.LocalDateTime.now();
}
