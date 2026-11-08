package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.dto.api.AuthStatusResponse;
import com.paulo.ecommerce.exception.EmailJaCadastradoException;
import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.service.RecuperacaoSenhaService;
import com.paulo.ecommerce.service.UsuarioService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
  private final UsuarioService usuarios;
  private final RecuperacaoSenhaService recuperacao;
  private final Cache<String, Integer> tentativas = CacheBuilder.newBuilder()
      .maximumSize(10_000).expireAfterWrite(Duration.ofMinutes(15)).build();

  public AuthApiController(UsuarioService usuarios, RecuperacaoSenhaService recuperacao) {
    this.usuarios = usuarios;
    this.recuperacao = recuperacao;
  }

  @GetMapping("/me")
  public AuthStatusResponse me(@AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails == null) {
      return new AuthStatusResponse(false, null, null, null, null, null, null, null, recuperacao.habilitada());
    }

    var usuario = usuarios.buscarPorEmail(userDetails.getUsername());
    return new AuthStatusResponse(true, usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getTemaPasse(),
        usuario.getAvatarPasse(), usuario.getBadgePasse(), usuario.getRole().name(), recuperacao.habilitada());
  }

  public record Cadastro(@NotBlank @Size(max = 100) String nome,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 72) String senha) {}

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public void cadastrar(@Valid @RequestBody Cadastro request) {
    var usuario = new Usuario();
    usuario.setNome(request.nome());
    usuario.setEmail(request.email());
    usuario.setSenha(request.senha());
    try {
      usuarios.cadastrar(usuario);
    } catch (EmailJaCadastradoException | DataIntegrityViolationException error) {
      throw new IllegalArgumentException("Este e-mail já está cadastrado.");
    }
  }

  public record PedidoRecuperacao(@NotBlank @Email @Size(max = 254) String email) {}
  public record Redefinicao(@NotBlank @Size(max = 64) String token,
      @NotBlank @Size(min = 12, max = 72) String senha) {}

  @PostMapping("/password/forgot")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void solicitar(@Valid @RequestBody PedidoRecuperacao request, HttpServletRequest http) {
    if (permitir("solicitar:" + http.getRemoteAddr())) recuperacao.solicitar(request.email());
  }

  @PostMapping("/password/reset")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void redefinir(@Valid @RequestBody Redefinicao request, HttpServletRequest http) {
    if (!permitir("redefinir:" + http.getRemoteAddr()))
      throw new IllegalArgumentException("Muitas tentativas. Aguarde antes de tentar novamente.");
    recuperacao.redefinir(request.token(), request.senha());
  }

  private boolean permitir(String chave) {
    return tentativas.asMap().compute(chave, (k, n) -> n == null ? 1 : Math.min(n + 1, 6)) <= 5;
  }
}
