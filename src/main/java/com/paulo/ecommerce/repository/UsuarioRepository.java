package com.paulo.ecommerce.repository;

import com.paulo.ecommerce.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select x from Usuario x where x.email = :value")
    java.util.Optional<Usuario> findLockedByEmail(@org.springframework.data.repository.query.Param("value") String value);
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
