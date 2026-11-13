package com.paulo.ecommerce.model;
import jakarta.persistence.*;
@Entity
@Table(name="pedido_devolucoes",uniqueConstraints=@UniqueConstraint(columnNames={"pedido_id","chave"}))
public class PedidoDevolucao {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long pedidoId;
 @Column(nullable=false) public Long itemId;
 @Column(nullable=false) public int quantidade;
 @Column(nullable=false,length=20) public String tipo;
 @Column(nullable=false,length=20) public String estado="SOLICITADA";
 @Column(nullable=false,length=1000) public String motivo;
 @Column(nullable=false,length=80) public String chave;
 @Column(length=1000) public String resposta;
 @Column(nullable=false) public int reposta;
 @Column(nullable=false) public java.time.LocalDateTime criadoEm=java.time.LocalDateTime.now();
 public java.time.LocalDateTime recebidoEm;
 @Column(length=254) public String conferidoPor;
}
