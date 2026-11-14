package com.paulo.ecommerce.model;
import jakarta.persistence.*;
@Entity
@Table(name="estoque_reservas", uniqueConstraints=@UniqueConstraint(columnNames={"produto_id","referencia"}))
public class EstoqueReserva {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long produtoId;
 @Column(nullable=false,length=100) public String referencia;
 @Column(nullable=false) public int quantidade;
 @Column(nullable=false,length=20) public String estado = "RESERVADO";
}
