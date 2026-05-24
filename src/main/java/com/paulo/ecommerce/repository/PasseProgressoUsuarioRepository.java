package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.PasseProgressoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasseProgressoUsuarioRepository extends JpaRepository<PasseProgressoUsuario, Long> {
    Optional<PasseProgressoUsuario> findByUsuarioIdAndTemporadaId(Long usuarioId, Long temporadaId);
}
