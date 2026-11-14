package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.RecuperacaoSenha;
import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.repository.RecuperacaoSenhaRepository;
import com.paulo.ecommerce.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.core.env.Environment;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class RecuperacaoSenhaService {
  private final UsuarioRepository usuarios;
  private final RecuperacaoSenhaRepository tokens;
  private final EmailService email;
  private final PasswordEncoder encoder;
  private final SessionRegistry sessions;
  private final Environment environment;
  private final SecureRandom random = new SecureRandom();

  public RecuperacaoSenhaService(UsuarioRepository usuarios, RecuperacaoSenhaRepository tokens,
      EmailService email, PasswordEncoder encoder, SessionRegistry sessions, Environment environment) {
    this.usuarios = usuarios;
    this.tokens = tokens;
    this.email = email;
    this.encoder = encoder;
    this.sessions = sessions;
    this.environment = environment;
  }

  public boolean habilitada() {
    return environment.matchesProfiles("local & !prod")
        && environment.getProperty("app.local-demo-enabled", Boolean.class, false);
  }

  @Transactional
  public void solicitar(String endereco) {
    if (!habilitada()) return;
    var encontrados = usuarios.findByEmailIgnoreCase(endereco.trim());
    if (encontrados.size() != 1) return;
    Usuario usuario = usuarios.findLockedById(encontrados.get(0).getId()).orElseThrow();
    LocalDateTime agora = LocalDateTime.now();
    var registro = tokens.findById(usuario.getId()).orElseGet(RecuperacaoSenha::new);
    if (registro.expiraEm != null && registro.expiraEm.isAfter(agora.plusMinutes(14))) return;
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    registro.usuarioId = usuario.getId();
    registro.hash = hash(token);
    registro.expiraEm = agora.plusMinutes(15);
    tokens.save(registro);
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        email.enviarRecuperacaoSenha(usuario.getEmail(), token);
      }
    });
  }

  @Transactional
  public void redefinir(String token, String senha) {
    if (!habilitada() || token == null || !token.matches("[A-Za-z0-9_-]{43}"))
      throw linkInvalido();
    UsuarioService.validarSenha(senha);
    String hash = hash(token);
    Long id = tokens.identificarUsuario(hash).orElseThrow(RecuperacaoSenhaService::linkInvalido);
    Usuario usuario = usuarios.findLockedById(id).orElseThrow(RecuperacaoSenhaService::linkInvalido);
    var registro = tokens.findById(id).orElseThrow(RecuperacaoSenhaService::linkInvalido);
    if (!Objects.equals(registro.hash, hash) || !registro.expiraEm.isAfter(LocalDateTime.now()))
      throw linkInvalido();
    usuario.setSenha(encoder.encode(senha));
    tokens.delete(registro);
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        sessions.getAllPrincipals().stream()
            .filter(p -> p instanceof UserDetails details && usuario.getEmail().equals(details.getUsername()))
            .flatMap(p -> sessions.getAllSessions(p, false).stream())
            .forEach(session -> session.expireNow());
      }
    });
  }

  private static IllegalArgumentException linkInvalido() {
    return new IllegalArgumentException("Este link expirou ou já foi usado. Solicite outro.");
  }

  private static String hash(String token) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }
}
