package com.paulo.ecommerce.service;

import com.paulo.ecommerce.exception.EmailJaCadastradoException;
import com.paulo.ecommerce.exception.RecursoNaoEncontradoException;
import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;
    private final EmailService emailService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder   = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public Usuario cadastrar(Usuario usuario) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new EmailJaCadastradoException(usuario.getEmail());

        }
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setRole(Usuario.Role.CLIENTE);
        emailService.enviarBoasVindas(usuario);

        return usuarioRepository.save(usuario);
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Usuário", 0L));
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Usuário", id));
    }

    @Transactional
    public Usuario atualizar(Long id, Usuario dados) {
        Usuario usuario = buscarPorId(id);
        usuario.setNome(dados.getNome());
        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dados.getSenha()));
        }
        return usuarioRepository.save(usuario);
    }
}