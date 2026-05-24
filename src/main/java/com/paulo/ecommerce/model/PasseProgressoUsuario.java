package com.paulo.ecommerce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "passe_progresso_usuarios",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "temporada_id"}))
public class PasseProgressoUsuario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temporada_id", nullable = false)
    private PasseTemporada temporada;

    @Column(nullable = false)
    private Integer xpTotal = 0;

    @Column(nullable = false)
    private Integer nivelAtual = 1;

    private LocalDateTime ultimoGanhoXpEm;

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

    public Integer getXpTotal() {
        return xpTotal;
    }

    public void setXpTotal(Integer xpTotal) {
        this.xpTotal = xpTotal;
    }

    public Integer getNivelAtual() {
        return nivelAtual;
    }

    public void setNivelAtual(Integer nivelAtual) {
        this.nivelAtual = nivelAtual;
    }

    public LocalDateTime getUltimoGanhoXpEm() {
        return ultimoGanhoXpEm;
    }

    public void setUltimoGanhoXpEm(LocalDateTime ultimoGanhoXpEm) {
        this.ultimoGanhoXpEm = ultimoGanhoXpEm;
    }
}
