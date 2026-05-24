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

@Entity
@Table(name = "passe_recompensas_nivel",
        uniqueConstraints = @UniqueConstraint(columnNames = {"temporada_id", "nivel"}))
public class PasseRecompensaNivel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temporada_id", nullable = false)
    private PasseTemporada temporada;

    @Column(nullable = false)
    private Integer nivel;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(nullable = false, length = 60)
    private String tipoDigital;

    @Column(nullable = false, length = 160)
    private String codigo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PasseTemporada getTemporada() {
        return temporada;
    }

    public void setTemporada(PasseTemporada temporada) {
        this.temporada = temporada;
    }

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipoDigital() {
        return tipoDigital;
    }

    public void setTipoDigital(String tipoDigital) {
        this.tipoDigital = tipoDigital;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
