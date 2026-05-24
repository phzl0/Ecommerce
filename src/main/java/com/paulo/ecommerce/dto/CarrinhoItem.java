package com.paulo.ecommerce.dto;

import java.math.BigDecimal;
import java.io.Serializable;

public class CarrinhoItem implements Serializable {

    private Long produtoId;
    private String nome;
    private String imagemUrl;
    private BigDecimal precoUnitario;
    private Integer quantidade;

    public CarrinhoItem() {}

    public CarrinhoItem(Long produtoId, String nome, String imagemUrl,
                        BigDecimal precoUnitario, Integer quantidade) {
        this.produtoId    = produtoId;
        this.nome         = nome;
        this.imagemUrl    = imagemUrl;
        this.precoUnitario = precoUnitario;
        this.quantidade   = quantidade;
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    // Getters e Setters
    public Long getProdutoId()               { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }
    public String getNome()                  { return nome; }
    public void setNome(String nome)         { this.nome = nome; }
    public String getImagemUrl()             { return imagemUrl; }
    public void setImagemUrl(String url)     { this.imagemUrl = url; }
    public BigDecimal getPrecoUnitario()     { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal p){ this.precoUnitario = p; }
    public Integer getQuantidade()           { return quantidade; }
    public void setQuantidade(Integer q)     { this.quantidade = q; }
}