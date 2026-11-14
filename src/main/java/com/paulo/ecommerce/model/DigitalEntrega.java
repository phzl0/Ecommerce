package com.paulo.ecommerce.model;
import jakarta.persistence.*;
@Entity
@Table(name="digital_entregas", uniqueConstraints={@UniqueConstraint(columnNames={"item_id","unidade"}),@UniqueConstraint(columnNames="codigo_id")})
public class DigitalEntrega {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long usuarioId;
 @Column(nullable=false) public Long pedidoId;
 @Column(nullable=false) public Long produtoId;
 @Column(nullable=false) public Long itemId;
 @Column(nullable=false) public int unidade;
 @Column(nullable=false,length=255) public String titulo;
 @Column(nullable=false,length=20) public String modo;
 @Column(nullable=false,length=20) public String estado = "PENDENTE";
 public Long arquivoId;
 public Long codigoId;
 public java.time.LocalDateTime liberadoEm;
 public java.time.LocalDateTime acessadoEm;
}
