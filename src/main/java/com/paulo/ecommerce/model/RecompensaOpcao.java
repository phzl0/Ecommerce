package com.paulo.ecommerce.model;

import jakarta.persistence.*;

@Entity
@Table(name="recompensa_opcoes", uniqueConstraints=@UniqueConstraint(columnNames={"recompensa_id", "produto_id"}))
public class RecompensaOpcao {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(optional=false) public PasseRecompensaNivel recompensa;
  @ManyToOne(optional=false) public Produto produto;
}
