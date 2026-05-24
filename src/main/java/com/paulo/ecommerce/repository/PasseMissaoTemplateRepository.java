package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.PasseMissaoTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasseMissaoTemplateRepository extends JpaRepository<PasseMissaoTemplate, Long> {
    List<PasseMissaoTemplate> findByTemporadaIdAndAtivaTrueOrderByPeriodicidadeAscOrdemExibicaoAsc(Long temporadaId);
}
