package com.paulo.ecommerce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "enderecos_entrega")
public class EnderecoEntrega {
  @Id public Long usuarioId;
  @Column(nullable = false, length = 9) public String cep;
  @Column(nullable = false, length = 120) public String endereco;
  @Column(nullable = false, length = 20) public String numero;
  @Column(length = 80) public String complemento;
  @Column(nullable = false, length = 80) public String bairro;
  @Column(nullable = false, length = 80) public String cidade;
  @Column(nullable = false, length = 2) public String estado;
}
