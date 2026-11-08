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
  public Usuario cadastrar(Usuario dados) {
    String nome = dados.getNome().trim();
    String email = dados.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
    validarSenha(dados.getSenha());
    if (nome.length() < 2 || nome.length() > 100)
      throw new IllegalArgumentException("Informe um nome de 2 a 100 caracteres.");
    if (email.length() > 254) throw new IllegalArgumentException("Informe um e-mail válido.");
    if (com.paulo.ecommerce.config.LocalDevelopmentConfig.ADMIN_EMAIL.equals(email)
        || usuarioRepository.existsByEmailIgnoreCase(email)) {
      throw new EmailJaCadastradoException(email);
    }

    Usuario novo = new Usuario();
    novo.setNome(nome);
    novo.setEmail(email);
    novo.setSenha(passwordEncoder.encode(dados.getSenha()));
    novo.setRole(Usuario.Role.CLIENTE);
    Usuario salvo = usuarioRepository.saveAndFlush(novo);
    org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
        new org.springframework.transaction.support.TransactionSynchronization() {
          @Override
          public void afterCommit() {
            emailService.enviarBoasVindas(salvo);
          }
        });
    return salvo;
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
