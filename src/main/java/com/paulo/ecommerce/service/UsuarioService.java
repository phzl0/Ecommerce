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
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  public UsuarioService(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      EmailService emailService) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
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
    return usuarioRepository
        .findByEmail(email)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", 0L));
  }

  public Usuario buscarPorId(Long id) {
    return usuarioRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
  }

  @Transactional
  public Usuario atualizarPerfil(Long id, String nome, String novaSenha) {
    Usuario usuario = buscarPorId(id);
    if (nome != null) {
      nome = nome.trim();
      if (nome.length() < 2 || nome.length() > 100)
        throw new IllegalArgumentException("Nome deve ter entre 2 e 100 caracteres.");
      usuario.setNome(nome);
    }
    if (novaSenha != null && !novaSenha.isBlank()) {
      if (novaSenha.length() < 12 || novaSenha.length() > 72)
        throw new IllegalArgumentException("A nova senha deve ter entre 12 e 72 caracteres.");
      usuario.setSenha(passwordEncoder.encode(novaSenha));
    }
    return usuarioRepository.save(usuario);
  }
}
