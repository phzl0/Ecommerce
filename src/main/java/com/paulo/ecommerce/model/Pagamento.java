package com.paulo.ecommerce.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos")
public class Pagamento extends BaseEntity {
  @Column(length = 2048)
  private String checkoutUrl;

  public String getCheckoutUrl() {
    return checkoutUrl;
  }

  public void setCheckoutUrl(String value) {
    checkoutUrl = value;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "pedido_id", nullable = false)
  private Pedido pedido;

  @Column(name = "mp_payment_id")
  private String mpPaymentId;

  @Column(name = "mp_preference_id")
  private String mpPreferenceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusPagamento status = StatusPagamento.PENDENTE;

  @Enumerated(EnumType.STRING)
  private MetodoPagamento metodo;

  @Column(precision = 10, scale = 2)
  private BigDecimal valor;

  private LocalDateTime pagoEm;

  public enum StatusPagamento {
    PENDENTE,
    APROVADO,
    RECUSADO,
    CANCELADO,
    REEMBOLSADO
  }

  public enum MetodoPagamento {
    PIX,
    BOLETO,
    CARTAO_CREDITO,
    CARTAO_DEBITO
  }

  // Getters e Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Pedido getPedido() {
    return pedido;
  }

  public void setPedido(Pedido pedido) {
    this.pedido = pedido;
  }

  public String getMpPaymentId() {
    return mpPaymentId;
  }

  public void setMpPaymentId(String id) {
    this.mpPaymentId = id;
  }

  public String getMpPreferenceId() {
    return mpPreferenceId;
  }

  public void setMpPreferenceId(String id) {
    this.mpPreferenceId = id;
  }

  public StatusPagamento getStatus() {
    return status;
  }

  public void setStatus(StatusPagamento s) {
    this.status = s;
  }

  public MetodoPagamento getMetodo() {
    return metodo;
  }

  public void setMetodo(MetodoPagamento m) {
    this.metodo = m;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public void setValor(BigDecimal valor) {
    this.valor = valor;
  }

  public LocalDateTime getPagoEm() {
    return pagoEm;
  }

  public void setPagoEm(LocalDateTime pagoEm) {
    this.pagoEm = pagoEm;
  }
}
