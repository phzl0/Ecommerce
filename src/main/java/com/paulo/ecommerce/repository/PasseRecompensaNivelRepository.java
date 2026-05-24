package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.PasseRecompensaNivel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasseRecompensaNivelRepository extends JpaRepository<PasseRecompensaNivel, Long> {
    List<PasseRecompensaNivel> findByTemporadaIdOrderByNivelAsc(Long temporadaId);
}
