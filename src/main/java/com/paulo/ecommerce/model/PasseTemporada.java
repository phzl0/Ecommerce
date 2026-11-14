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

    @Column(nullable=false,length=20) private String estado = "PUBLICADO";
    @Column(nullable=false) private boolean gerenciada;
    @Column(precision=10,scale=2) private java.math.BigDecimal preco;
    @Column(nullable=false) private int previsaoGratuitos = 0;
    @Column(nullable=false) private int previsaoPremium = 0;
    @Column(length=120) private String publicadoPor;
    private java.time.LocalDateTime publicadoEm;
    public String getEstado(){return estado;} public void setEstado(String v){estado=v;}
    public boolean isGerenciada(){return gerenciada;} public void setGerenciada(boolean v){gerenciada=v;}
    public java.math.BigDecimal getPreco(){return preco;} public void setPreco(java.math.BigDecimal v){preco=v;}
    public int getPrevisaoGratuitos(){return previsaoGratuitos;} public void setPrevisaoGratuitos(int v){previsaoGratuitos=v;}
    public int getPrevisaoPremium(){return previsaoPremium;} public void setPrevisaoPremium(int v){previsaoPremium=v;}
    public String getPublicadoPor(){return publicadoPor;} public void setPublicadoPor(String v){publicadoPor=v;}
    public java.time.LocalDateTime getPublicadoEm(){return publicadoEm;} public void setPublicadoEm(java.time.LocalDateTime v){publicadoEm=v;}
    public boolean disponivel(){var hoje=java.time.LocalDate.now();return Boolean.TRUE.equals(ativa)&&"PUBLICADO".equals(estado)&&!hoje.isBefore(inicioEm)&&!hoje.isAfter(fimEm);}

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
