package com.paulo.ecommerce.model;
import jakarta.persistence.*;
@Entity
@Table(name="digital_arquivos")
public class DigitalArquivo {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long produtoId;
 @Column(nullable=false,length=160) public String nome;
 @Column(nullable=false, length=5242880) public byte[] dados;
 @Column(nullable=false) public java.time.LocalDateTime criadoEm = java.time.LocalDateTime.now();
}
