package com.paulo.ecommerce.model;
import jakarta.persistence.*;
@Entity
@Table(name="pedido_expedicoes")
public class PedidoExpedicao {
 @Id public Long pedidoId;
 @Column(nullable=false,length=20) public String etapa="SEPARANDO";
 @Column(length=100) public String transportadora;
 @Column(length=80) public String codigo;
 @Column(length=1000) public String url;
 @Column(nullable=false) public java.time.LocalDateTime iniciadoEm=java.time.LocalDateTime.now();
 public java.time.LocalDateTime conferidoEm;
 @Column(length=254) public String conferidoPor;
 public java.time.LocalDateTime enviadoEm;
 public java.time.LocalDateTime entregueEm;
}
