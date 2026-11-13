package com.paulo.ecommerce.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name="pedido_reembolsos",uniqueConstraints=@UniqueConstraint(columnNames={"pedidoId","chave"}))
public class PedidoReembolso {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long pedidoId;
 @Column(nullable=false,precision=10,scale=2) public BigDecimal valor;
 @Column(nullable=false,length=20) public String estado="PENDENTE";
 @Column(nullable=false,length=80) public String chave;
 @Column(nullable=false,length=1000) public String motivo;
 @Column(nullable=false,length=254) public String solicitadoPor;
 @Column(length=254) public String confirmadoPor;
 @Column(nullable=false) public LocalDateTime criadoEm=LocalDateTime.now();
 public LocalDateTime confirmadoEm;
 @Column(nullable=false,length=8000) public String impacto;
 public boolean analiseBeneficios;
 @Column(length=1500) public String resolucaoAnalise;
 @Column(length=254) public String analisadoPor;
 public LocalDateTime analisadoEm;
}
