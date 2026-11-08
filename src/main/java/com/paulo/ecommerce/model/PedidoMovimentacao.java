package com.paulo.ecommerce.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedido_movimentacoes")
public class PedidoMovimentacao {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long pedidoId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private Pedido.Status status;

  @Column(nullable = false, length = 120)
  private String descricao;

  @Column(nullable = false)
  private LocalDateTime ocorridoEm;

  protected PedidoMovimentacao() {}

  public PedidoMovimentacao(Long pedidoId, Pedido.Status status, String descricao) {
    this.pedidoId = pedidoId;
    this.status = status;
    this.descricao = descricao;
    this.ocorridoEm = LocalDateTime.now();
  }

  public Pedido.Status getStatus() { return status; }
  public String getDescricao() { return descricao; }
  public LocalDateTime getOcorridoEm() { return ocorridoEm; }
}
