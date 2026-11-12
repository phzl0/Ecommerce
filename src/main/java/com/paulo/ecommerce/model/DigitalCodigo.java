package com.paulo.ecommerce.model;
import jakarta.persistence.*;
@Entity
@Table(name="digital_codigos", uniqueConstraints=@UniqueConstraint(columnNames={"produto_id","hash"}))
public class DigitalCodigo {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long produtoId;
 @Column(nullable=false,length=500) public String valor;
 @Column(nullable=false,length=64) public String hash;
 @Column(nullable=false,length=20) public String estado = "DISPONIVEL";
}
