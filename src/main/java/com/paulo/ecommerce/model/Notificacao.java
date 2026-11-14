package com.paulo.ecommerce.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes")
public class Notificacao {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false) public Long usuarioId;
  @Column(nullable = false, length = 140) public String titulo;
  @Column(nullable = false, length = 300) public String mensagem;
  @Column(nullable = false, length = 120) public String destino;
  @Column(nullable = false) public LocalDateTime criadoEm;
  @Column(nullable = false) public boolean lida;
}
