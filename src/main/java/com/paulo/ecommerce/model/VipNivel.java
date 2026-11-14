package com.paulo.ecommerce.model;

import jakarta.persistence.*;

@Entity
@Table(name="vip_niveis")
public class VipNivel {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false, length=80) public String nome;
  public long pontos;
  @Column(nullable=false, length=120) public String premio;
  @ManyToOne public Produto produto;
  public boolean ativo = true;
}
