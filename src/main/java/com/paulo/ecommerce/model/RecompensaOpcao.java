package com.paulo.ecommerce.model;

import jakarta.persistence.*;

@Entity
@Table(name="recompensa_opcoes", uniqueConstraints=@UniqueConstraint(columnNames={"recompensa_id", "produto_id"}))
public class RecompensaOpcao {
  @Column(nullable=false,precision=10,scale=2) public java.math.BigDecimal custo = java.math.BigDecimal.ZERO;
  @Column(nullable=false,precision=10,scale=2) public java.math.BigDecimal frete = java.math.BigDecimal.ZERO;
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(optional=false) public PasseRecompensaNivel recompensa;
  @ManyToOne(optional=false) public Produto produto;
}
