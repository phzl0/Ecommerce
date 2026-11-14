package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.PasseTemporada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasseTemporadaRepository extends JpaRepository<PasseTemporada, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from PasseTemporada t where t.id=:id")
    Optional<PasseTemporada> bloquear(Long id);
    java.util.List<PasseTemporada> findAllByOrderByInicioEmDesc();
    Optional<PasseTemporada> findBySlug(String slug);
    Optional<PasseTemporada> findByAnoAndMes(Integer ano, Integer mes);
}
