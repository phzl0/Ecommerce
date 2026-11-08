package com.paulo.ecommerce.config;

import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("local & !prod")
@ConditionalOnProperty(name = "app.local-demo-enabled", havingValue = "true")
public class LocalDevelopmentConfig {
  public static final String ADMIN_EMAIL = "admin@level.local";

  @Bean
  CommandLineRunner administradorDeTeste(UsuarioRepository usuarios, PasswordEncoder encoder) {
    return args -> {
      if (usuarios.existsByEmailIgnoreCase(ADMIN_EMAIL)) return;
      var admin = new Usuario();
      admin.setEmail(ADMIN_EMAIL);
      admin.setNome("Admin local");
      admin.setRole(Usuario.Role.ADMIN);
      admin.setSenha(encoder.encode("AdminLocal123!"));
      usuarios.save(admin);
    };
  }
}
