package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.VipNivel;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VipNivelRepository extends JpaRepository<VipNivel, Long> {
  java.util.List<VipNivel> findByAtivoTrueOrderByPontosAsc();
}
