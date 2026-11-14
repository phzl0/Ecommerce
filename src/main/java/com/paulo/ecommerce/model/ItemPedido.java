package com.paulo.ecommerce.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "itens_pedido")
public class ItemPedido extends BaseEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20)
    private Produto.Tipo tipo = Produto.Tipo.FISICO;
    public Produto.Tipo getTipo() { return tipo; }
    public void setTipo(Produto.Tipo tipo) { this.tipo=tipo; }
    @Column(length=500) private String nomeSnapshot;
    @Column(length=40) private String skuSnapshot;
    private String imagemSnapshot;
    @Column(nullable=false) private int xpExtraSnapshot;
    public String getNomeSnapshot(){return nomeSnapshot==null?produto.nomeCompra():nomeSnapshot;}
    public String getSkuSnapshot(){return skuSnapshot==null?produto.getSku():skuSnapshot;}
    public String getImagemSnapshot(){return nomeSnapshot==null?produto.getImagemUrl():imagemSnapshot;}
    public int getXpExtraSnapshot(){return xpExtraSnapshot;}
    public void setXpExtraSnapshot(int v){xpExtraSnapshot=v;}
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; if(produto!=null&&nomeSnapshot==null){nomeSnapshot=produto.nomeCompra();skuSnapshot=produto.getSku();imagemSnapshot=produto.getImagemUrl();} }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}