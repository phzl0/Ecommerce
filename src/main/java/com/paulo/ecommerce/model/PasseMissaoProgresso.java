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
@Table(name = "passe_missoes_progresso",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "template_id", "periodo_inicio"}))
public class PasseMissaoProgresso extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temporada_id", nullable = false)
    private PasseTemporada temporada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private PasseMissaoTemplate template;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDateTime periodoInicio;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDateTime periodoFim;

    @Column(nullable = false)
    private Integer progressoAtual = 0;

    @Column(nullable = false)
    private Boolean concluida = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean recompensaEntregue = Boolean.FALSE;

    private LocalDateTime concluidaEm;

    private LocalDateTime recompensaEntregueEm;

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

    public PasseMissaoTemplate getTemplate() {
        return template;
    }

    public void setTemplate(PasseMissaoTemplate template) {
        this.template = template;
    }

    public LocalDateTime getPeriodoInicio() {
        return periodoInicio;
    }

    public void setPeriodoInicio(LocalDateTime periodoInicio) {
        this.periodoInicio = periodoInicio;
    }

    public LocalDateTime getPeriodoFim() {
        return periodoFim;
    }

    public void setPeriodoFim(LocalDateTime periodoFim) {
        this.periodoFim = periodoFim;
    }

    public Integer getProgressoAtual() {
        return progressoAtual;
    }

    public void setProgressoAtual(Integer progressoAtual) {
        this.progressoAtual = progressoAtual;
    }

    public Boolean getConcluida() {
        return concluida;
    }

    public void setConcluida(Boolean concluida) {
        this.concluida = concluida;
    }

    public Boolean getRecompensaEntregue() {
        return recompensaEntregue;
    }

    public void setRecompensaEntregue(Boolean recompensaEntregue) {
        this.recompensaEntregue = recompensaEntregue;
    }

    public LocalDateTime getConcluidaEm() {
        return concluidaEm;
    }

    public void setConcluidaEm(LocalDateTime concluidaEm) {
        this.concluidaEm = concluidaEm;
    }

    public LocalDateTime getRecompensaEntregueEm() {
        return recompensaEntregueEm;
    }

    public void setRecompensaEntregueEm(LocalDateTime recompensaEntregueEm) {
        this.recompensaEntregueEm = recompensaEntregueEm;
    }
}
