package com.paulo.ecommerce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "passe_temporadas")
public class PasseTemporada extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private Integer totalNiveis;

    @Column(nullable = false)
    private Integer xpPorNivel;

    @Column(nullable = false)
    private Boolean ativa = Boolean.TRUE;

    @Column(nullable = false)
    private LocalDate inicioEm;

    @Column(nullable = false)
    private LocalDate fimEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public Integer getTotalNiveis() {
        return totalNiveis;
    }

    public void setTotalNiveis(Integer totalNiveis) {
        this.totalNiveis = totalNiveis;
    }

    public Integer getXpPorNivel() {
        return xpPorNivel;
    }

    public void setXpPorNivel(Integer xpPorNivel) {
        this.xpPorNivel = xpPorNivel;
    }

    public Boolean getAtiva() {
        return ativa;
    }

    public void setAtiva(Boolean ativa) {
        this.ativa = ativa;
    }

    public LocalDate getInicioEm() {
        return inicioEm;
    }

    public void setInicioEm(LocalDate inicioEm) {
        this.inicioEm = inicioEm;
    }

    public LocalDate getFimEm() {
        return fimEm;
    }

    public void setFimEm(LocalDate fimEm) {
        this.fimEm = fimEm;
    }
}
