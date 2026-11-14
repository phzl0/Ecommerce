package com.paulo.ecommerce.model;
import jakarta.persistence.*;
@Entity
@Table(name="pedido_atendimentos",uniqueConstraints=@UniqueConstraint(columnNames={"pedido_id","chave"}))
public class PedidoAtendimento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long pedidoId;
 @Column(nullable=false,length=80) public String chave;
 @Column(nullable=false,length=2000) public String mensagem;
 @Column(nullable=false,length=254) public String autor;
 @Column(nullable=false) public boolean publico;
 @Column(nullable=false) public boolean cliente;
 @Column(nullable=false) public java.time.LocalDateTime criadoEm=java.time.LocalDateTime.now();
}
