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
    usuario.setNome(usuario.getNome().trim());
    usuario.setEmail(usuario.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
    validarSenha(usuario.getSenha());
    if (usuario.getNome().length() < 2 || usuario.getNome().length() > 100)
      throw new IllegalArgumentException("Informe um nome de 2 a 100 caracteres.");
    if (com.paulo.ecommerce.config.LocalDevelopmentConfig.ADMIN_EMAIL.equals(usuario.getEmail())
        || usuarioRepository.existsByEmailIgnoreCase(usuario.getEmail())) {
      throw new EmailJaCadastradoException(usuario.getEmail());
    }
    usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
    usuario.setRole(Usuario.Role.CLIENTE);
    emailService.enviarBoasVindas(usuario);

    return usuarioRepository.saveAndFlush(usuario);
  }

  public static void validarSenha(String senha) {
    if (senha == null || senha.isBlank() || senha.length() < 12
        || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException("Use uma senha com pelo menos 12 caracteres e até 72 bytes.");
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
      validarSenha(novaSenha);
      usuario.setSenha(passwordEncoder.encode(novaSenha));
    }
    return usuarioRepository.save(usuario);
  }
}
