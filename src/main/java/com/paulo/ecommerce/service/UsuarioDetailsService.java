package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final org.springframework.core.env.Environment environment;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository,
                                 org.springframework.core.env.Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.environment = environment;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (com.paulo.ecommerce.config.LocalDevelopmentConfig.ADMIN_EMAIL.equalsIgnoreCase(email.trim())
                && (!environment.matchesProfiles("local & !prod")
                    || !environment.getProperty("app.local-demo-enabled", Boolean.class, false))) {
            throw new UsernameNotFoundException("Usuário não encontrado.");
        }
        Usuario usuario = usuarioRepository.findByEmail(email.trim())
                .orElseGet(() -> {
                    var matches = usuarioRepository.findByEmailIgnoreCase(email.trim());
                    if (matches.size() == 1) return matches.get(0);
                    throw new UsernameNotFoundException("Usuário não encontrado.");
                });

        return new org.springframework.security.core.userdetails.User(
                usuario.getEmail(),
                usuario.getSenha(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()))
        );
    }
}