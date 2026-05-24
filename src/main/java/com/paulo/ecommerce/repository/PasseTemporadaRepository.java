package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.PasseTemporada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasseTemporadaRepository extends JpaRepository<PasseTemporada, Long> {
    Optional<PasseTemporada> findBySlug(String slug);
    Optional<PasseTemporada> findByAnoAndMes(Integer ano, Integer mes);
}
