package com.paulo.ecommerce.model;
import jakarta.persistence.*;
@Entity
@Table(name="estoque_movimentos")
public class EstoqueMovimento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long produtoId;
 @Column(nullable=false) public int quantidade;
 @Column(nullable=false) public int saldo;
 @Column(nullable=false,length=30) public String tipo;
 @Column(nullable=false,length=200) public String motivo;
 @Column(nullable=false,length=254) public String autor;
 @Column(nullable=false) public java.time.LocalDateTime criadoEm = java.time.LocalDateTime.now();
}
