package com.paulo.ecommerce.model;

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
@Table(name = "passe_recompensa_resgates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "recompensa_id"}))
public class PasseRecompensaResgate extends BaseEntity {
    @jakarta.persistence.Column(precision=10,scale=2) private java.math.BigDecimal valorCupom;
    @jakarta.persistence.Column(precision=10,scale=2) private java.math.BigDecimal minimoCupom;
    public java.math.BigDecimal getValorCupom(){return valorCupom;}
    public void setValorCupom(java.math.BigDecimal value){valorCupom=value;}
    public java.math.BigDecimal getMinimoCupom(){return minimoCupom;}
    public void setMinimoCupom(java.math.BigDecimal value){minimoCupom=value;}
    @jakarta.persistence.Column(unique=true,length=60) private String codigoEntrega;
    @ManyToOne(fetch=FetchType.LAZY) private com.paulo.ecommerce.model.Pedido pedidoUtilizado;
    public String getCodigoEntrega(){return codigoEntrega;}
    public void setCodigoEntrega(String value){codigoEntrega=value;}
    public com.paulo.ecommerce.model.Pedido getPedidoUtilizado(){return pedidoUtilizado;}
    public void setPedidoUtilizado(com.paulo.ecommerce.model.Pedido value){pedidoUtilizado=value;}


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
    @JoinColumn(name = "recompensa_id", nullable = false)
    private PasseRecompensaNivel recompensa;

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

    public PasseRecompensaNivel getRecompensa() {
        return recompensa;
    }

    public void setRecompensa(PasseRecompensaNivel recompensa) {
        this.recompensa = recompensa;
    }
}
