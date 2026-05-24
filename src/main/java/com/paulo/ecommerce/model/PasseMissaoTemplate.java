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
@Table(name = "passe_missoes_template",
        uniqueConstraints = @UniqueConstraint(columnNames = {"temporada_id", "codigo"}))
public class PasseMissaoTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temporada_id", nullable = false)
    private PasseTemporada temporada;

    @Column(nullable = false, length = 80)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PassePeriodicidade periodicidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PasseAcaoTipo acaoTipo;

    @Column(nullable = false)
    private Integer metaValor;

    @Column(nullable = false)
    private Integer xpRecompensa;

    @Column(nullable = false)
    private Integer ordemExibicao = 0;

    @Column(nullable = false)
    private Boolean ativa = Boolean.TRUE;

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

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
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

    public PassePeriodicidade getPeriodicidade() {
        return periodicidade;
    }

    public void setPeriodicidade(PassePeriodicidade periodicidade) {
        this.periodicidade = periodicidade;
    }

    public PasseAcaoTipo getAcaoTipo() {
        return acaoTipo;
    }

    public void setAcaoTipo(PasseAcaoTipo acaoTipo) {
        this.acaoTipo = acaoTipo;
    }

    public Integer getMetaValor() {
        return metaValor;
    }

    public void setMetaValor(Integer metaValor) {
        this.metaValor = metaValor;
    }

    public Integer getXpRecompensa() {
        return xpRecompensa;
    }

    public void setXpRecompensa(Integer xpRecompensa) {
        this.xpRecompensa = xpRecompensa;
    }

    public Integer getOrdemExibicao() {
        return ordemExibicao;
    }

    public void setOrdemExibicao(Integer ordemExibicao) {
        this.ordemExibicao = ordemExibicao;
    }

    public Boolean getAtiva() {
        return ativa;
    }

    public void setAtiva(Boolean ativa) {
        this.ativa = ativa;
    }
}
