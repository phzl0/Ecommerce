package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    interface Resumo {
        Long getId();
        String getNome();
        String getDescricao();
        Long getTotalProdutos();
    }

    @org.springframework.data.jpa.repository.Query("select c.id as id, c.nome as nome, c.descricao as descricao, count(p.id) as totalProdutos from Categoria c left join c.produtos p group by c.id, c.nome, c.descricao order by c.nome")
    java.util.List<Resumo> listarResumo();

    Optional<Categoria> findByNomeIgnoreCase(String nome);
}
