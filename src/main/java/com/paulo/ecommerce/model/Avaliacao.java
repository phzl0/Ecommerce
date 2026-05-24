package com.paulo.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "avaliacoes",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"usuario_id", "produto_id"}))
public class Avaliacao extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Min(1) @Max(5)
    @Column(nullable = false)
    private Integer nota;

    @Size(max = 500)
    private String comentario;

    // Getters e Setters
    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }
    public Usuario getUsuario()            { return usuario; }
    public void setUsuario(Usuario u)      { this.usuario = u; }
    public Produto getProduto()            { return produto; }
    public void setProduto(Produto p)      { this.produto = p; }
    public Integer getNota()               { return nota; }
    public void setNota(Integer nota)      { this.nota = nota; }
    public String getComentario()          { return comentario; }
    public void setComentario(String c)    { this.comentario = c; }
}