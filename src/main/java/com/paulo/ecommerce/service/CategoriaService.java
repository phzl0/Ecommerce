package com.paulo.ecommerce.service;

import com.paulo.ecommerce.exception.RecursoNaoEncontradoException;
import com.paulo.ecommerce.model.Categoria;
import com.paulo.ecommerce.repository.CategoriaRepository;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Cacheable("categorias")
    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Categoria", id));
    }

    @Transactional
    @CacheEvict(value = "categorias", allEntries = true)
    public Categoria salvar(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    @Transactional
    @CacheEvict(value = {"categorias", "produtos"}, allEntries = true)
    public void deletar(Long id) {
        categoriaRepository.deleteById(id);
    }
}