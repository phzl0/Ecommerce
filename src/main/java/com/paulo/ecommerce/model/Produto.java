package com.paulo.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
public class Produto extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nome;

    @Size(max = 40)
    @Column(length = 40, unique = true)
    private String sku;

    @Size(max = 80)
    @Column(length = 80)
    private String marca;

    @Size(max = 180)
    @Column(length = 180)
    private String descricaoCurta;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotNull
    @DecimalMin("0.01")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Min(0)
    @Column(nullable = false)
    private Integer estoque = 0;

    @Min(0) @Column(nullable=false) private Integer estoqueMinimo = 5;
    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(Integer valor) { estoqueMinimo = valor; }
    private String imagemUrl;

    private boolean ativo = true;

    private boolean destaque = false;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    public enum Tipo { FISICO, DIGITAL }
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo = Tipo.FISICO;
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public enum Publicacao { RASCUNHO, PUBLICADO, DESATIVADO }
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20)
    private Publicacao publicacao = Publicacao.PUBLICADO;
    private Long produtoPrincipalId;
    @Size(max=40) @Column(length=40) private String tamanho;
    @Size(max=40) @Column(length=40) private String cor;
    @Size(max=80) @Column(length=80) private String modelo;
    @Column(columnDefinition="TEXT") private String galeria;
    @DecimalMin("0.001") @DecimalMax("1000") @Column(precision=10,scale=3) private BigDecimal pesoKg;
    @DecimalMin("0.1") @DecimalMax("1000") @Column(precision=10,scale=2) private BigDecimal comprimentoCm;
    @DecimalMin("0.1") @DecimalMax("1000") @Column(precision=10,scale=2) private BigDecimal larguraCm;
    @DecimalMin("0.1") @DecimalMax("1000") @Column(precision=10,scale=2) private BigDecimal alturaCm;
    @Min(0) @Max(10000) @Column(nullable=false) private int xpExtra;
    @org.springframework.format.annotation.DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm") private java.time.LocalDateTime xpExtraInicio;
    @org.springframework.format.annotation.DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm") private java.time.LocalDateTime xpExtraFim;
    public Publicacao getPublicacao(){return publicacao;}
    public void setPublicacao(Publicacao v){publicacao=v;}
    public Long getProdutoPrincipalId(){return produtoPrincipalId;}
    public void setProdutoPrincipalId(Long v){produtoPrincipalId=v;}
    public String getTamanho(){return tamanho;} public void setTamanho(String v){tamanho=v;}
    public String getCor(){return cor;} public void setCor(String v){cor=v;}
    public String getModelo(){return modelo;} public void setModelo(String v){modelo=v;}
    public String getGaleria(){return galeria;} public void setGaleria(String v){galeria=v;}
    public BigDecimal getPesoKg(){return pesoKg;} public void setPesoKg(BigDecimal v){pesoKg=v;}
    public BigDecimal getComprimentoCm(){return comprimentoCm;} public void setComprimentoCm(BigDecimal v){comprimentoCm=v;}
    public BigDecimal getLarguraCm(){return larguraCm;} public void setLarguraCm(BigDecimal v){larguraCm=v;}
    public BigDecimal getAlturaCm(){return alturaCm;} public void setAlturaCm(BigDecimal v){alturaCm=v;}
    public int getXpExtra(){return xpExtra;} public void setXpExtra(int v){xpExtra=v;}
    public java.time.LocalDateTime getXpExtraInicio(){return xpExtraInicio;} public void setXpExtraInicio(java.time.LocalDateTime v){xpExtraInicio=v;}
    public java.time.LocalDateTime getXpExtraFim(){return xpExtraFim;} public void setXpExtraFim(java.time.LocalDateTime v){xpExtraFim=v;}
    public int xpExtraAtual(){var agora=java.time.LocalDateTime.now();return xpExtra>0&&xpExtraInicio!=null&&xpExtraFim!=null&&!agora.isBefore(xpExtraInicio)&&agora.isBefore(xpExtraFim)?xpExtra:0;}
    public boolean publicado(){return ativo&&publicacao==Publicacao.PUBLICADO;}
    public String opcao(){return java.util.stream.Stream.of(tamanho,cor,modelo).filter(v->v!=null&&!v.isBlank()).collect(java.util.stream.Collectors.joining(" · "));}
    public String nomeCompra(){return nome+(opcao().isBlank()?"":" · "+opcao());}
    public java.util.List<String> imagens(){var imagens=new java.util.LinkedHashSet<String>();if(imagemUrl!=null&&!imagemUrl.isBlank())imagens.add(imagemUrl);if(galeria!=null)galeria.lines().filter(v->!v.isBlank()).forEach(imagens::add);return java.util.List.copyOf(imagens);}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getDescricaoCurta() { return descricaoCurta; }
    public void setDescricaoCurta(String descricaoCurta) { this.descricaoCurta = descricaoCurta; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public Integer getEstoque() { return estoque; }
    public void setEstoque(Integer estoque) { this.estoque = estoque; }
    public String getImagemUrl() { return imagemUrl; }
    public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public boolean isDestaque() { return destaque; }
    public void setDestaque(boolean destaque) { this.destaque = destaque; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
}
