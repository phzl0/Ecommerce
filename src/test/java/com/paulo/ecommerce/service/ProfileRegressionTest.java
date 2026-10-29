package com.paulo.ecommerce.service;
import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ProfileRegressionTest {
 @Test void changingNamePreservesPasswordHash() {
  var repo=mock(UsuarioRepository.class);var encoder=new BCryptPasswordEncoder(4);
  var user=new Usuario();user.setId(1L);user.setSenha(encoder.encode("a-long-original-password"));
  String hash=user.getSenha();when(repo.findById(1L)).thenReturn(Optional.of(user));
  when(repo.save(user)).thenReturn(user);
  var service=new UsuarioService(repo,encoder,mock(EmailService.class));
  service.atualizarPerfil(1L,"Novo nome",null);
  assertEquals(hash,user.getSenha());assertTrue(encoder.matches("a-long-original-password",user.getSenha()));
  service.atualizarPerfil(1L,"Novo nome","another-long-password");
  assertTrue(encoder.matches("another-long-password",user.getSenha()));
 }
}
