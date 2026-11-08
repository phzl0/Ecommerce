package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.model.EnderecoEntrega;
import com.paulo.ecommerce.repository.EnderecoEntregaRepository;
import com.paulo.ecommerce.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account/address")
public class EnderecoEntregaController {
  private final EnderecoEntregaRepository enderecos;
  private final UsuarioRepository usuarios;

  public EnderecoEntregaController(EnderecoEntregaRepository enderecos, UsuarioRepository usuarios) {
    this.enderecos = enderecos;
    this.usuarios = usuarios;
  }

  public record Dados(
      @NotBlank @Pattern(regexp = "[0-9]{5}-?[0-9]{3}") String cep,
      @NotBlank @Size(max = 120) String endereco,
      @NotBlank @Size(max = 20) String numero,
      @Size(max = 80) String complemento,
      @NotBlank @Size(max = 80) String bairro,
      @NotBlank @Size(max = 80) String cidade,
      @NotBlank @Pattern(regexp = "(?i)AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO") String estado) {}

  @GetMapping
  public ResponseEntity<Dados> obter(@AuthenticationPrincipal UserDetails user) {
    var usuario = usuarios.findByEmail(user.getUsername()).orElseThrow();
    return enderecos.findById(usuario.getId()).map(this::dados).map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PutMapping
  @Transactional
  public Dados salvar(@AuthenticationPrincipal UserDetails user, @Valid @RequestBody Dados entrada) {
    var usuario = usuarios.findLockedByEmail(user.getUsername()).orElseThrow();
    var endereco = enderecos.findById(usuario.getId()).orElseGet(EnderecoEntrega::new);
    endereco.usuarioId = usuario.getId();
    endereco.cep = entrada.cep().replace("-", "");
    endereco.endereco = entrada.endereco().trim();
    endereco.numero = entrada.numero().trim();
    endereco.complemento = entrada.complemento() == null ? "" : entrada.complemento().trim();
    endereco.bairro = entrada.bairro().trim();
    endereco.cidade = entrada.cidade().trim();
    endereco.estado = entrada.estado().toUpperCase(Locale.ROOT);
    return dados(enderecos.save(endereco));
  }

  private Dados dados(EnderecoEntrega endereco) {
    return new Dados(endereco.cep, endereco.endereco, endereco.numero, endereco.complemento,
        endereco.bairro, endereco.cidade, endereco.estado);
  }
}
