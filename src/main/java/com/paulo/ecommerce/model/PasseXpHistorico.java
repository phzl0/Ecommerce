package com.paulo.ecommerce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "passe_xp_historico",
    uniqueConstraints =
        @UniqueConstraint(
            columnNames = {"usuario_id", "temporada_id", "origem_tipo", "referencia"}))
public class PasseXpHistorico extends BaseEntity {
  @Column(nullable = false, columnDefinition = "boolean default false")
  private boolean estornado;

  public boolean isEstornado() {
    return estornado;
  }

  public void setEstornado(boolean value) {
    estornado = value;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "temporada_id", nullable = false)
  private PasseTemporada temporada;

  @Enumerated(EnumType.STRING)
  @Column(name = "origem_tipo", nullable = false, length = 20)
  private PasseOrigemXpTipo origemTipo;

  @Column(nullable = false, length = 160)
  private String referencia;

  @Column(nullable = false)
  private Integer xpGanho;

  @Column(nullable = false, length = 255)
  private String descricao;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public PasseTemporada getTemporada() {
    return temporada;
  }

  public void setTemporada(PasseTemporada temporada) {
    this.temporada = temporada;
  }

  public PasseOrigemXpTipo getOrigemTipo() {
    return origemTipo;
  }

  public void setOrigemTipo(PasseOrigemXpTipo origemTipo) {
    this.origemTipo = origemTipo;
  }

  public String getReferencia() {
    return referencia;
  }

  public void setReferencia(String referencia) {
    this.referencia = referencia;
  }

  public Integer getXpGanho() {
    return xpGanho;
  }

  public void setXpGanho(Integer xpGanho) {
    this.xpGanho = xpGanho;
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String descricao) {
    this.descricao = descricao;
  }
}
