package com.paulo.ecommerce.config;

import com.paulo.ecommerce.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                // ── CSRF ──────────────────────────────────────
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/pagamento/webhook")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                )
                // ── Headers de segurança ──────────────────────
                .headers(headers -> headers
                        .contentTypeOptions(ct -> {})
                        .frameOptions(fo -> fo.sameOrigin())
                        .referrerPolicy(rp ->
                                rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self' 'unsafe-inline' https:; " +
                                                "style-src 'self' 'unsafe-inline' https:; " +
                                                "img-src 'self' data: https:; " +
                                                "font-src 'self' data: https:; " +
                                                "connect-src 'self' https:;"
                                ))
                )
                // ── Autorização de rotas ──────────────────────
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/produtos", "/produtos/**",
                                "/categorias/**", "/catalogo", "/produto/**", "/sacola", "/conta", "/checkout", "/login", "/cadastro", "/pagamento/retorno", "/passe", "/passe/missoes").permitAll()
                        .requestMatchers("/auth/**", "/css/**",
                                "/js/**", "/images/**", "/app/**", "/index.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/store/products/*/reviews").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers("/api/cep/**", "/api/store/**", "/api/auth/me").permitAll()
                        .requestMatchers("/api/checkout/**").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers("/api/account/**").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers("/api/pass/**").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers("/pagamento/webhook").permitAll()
                        .requestMatchers("/pagamento/sucesso",
                                "/pagamento/falha",
                                "/pagamento/pendente").permitAll()
                        .requestMatchers("/pagamento/**").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers("/avaliacoes/**").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/carrinho/**", "/pedidos/**",
                                "/perfil/**").hasAnyRole("CLIENTE", "ADMIN")
                        .anyRequest().authenticated()
                )
                // ── Login ─────────────────────────────────────
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .successHandler((request, response, authentication) -> {
                            String returnUrl = request.getParameter("returnUrl");
                            if (returnUrl != null && returnUrl.startsWith("/") && !returnUrl.startsWith("//") && !returnUrl.startsWith("/\\\\")) {
                                response.sendRedirect(returnUrl);
                                return;
                            }
                            response.sendRedirect("/");
                        })
                        .failureHandler((request, response, exception) -> {
                            String returnUrl = request.getParameter("returnUrl");
                            String redirect = "/login?erro=true";
                            if (returnUrl != null && returnUrl.startsWith("/") && !returnUrl.startsWith("//") && !returnUrl.startsWith("/\\\\")) {
                                redirect += "&returnUrl=" + java.net.URLEncoder.encode(
                                        returnUrl,
                                        java.nio.charset.StandardCharsets.UTF_8
                                );
                            }
                            new SimpleUrlAuthenticationFailureHandler(redirect)
                                    .onAuthenticationFailure(request, response, exception);
                        })
                        .permitAll()
                )
                // ── Logout ────────────────────────────────────
                .logout(logout -> logout
                        .logoutRequestMatcher(
                                new org.springframework.security.web.util.matcher
                                        .AntPathRequestMatcher("/auth/logout"))
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .permitAll()
                )
                // ── Sessão ────────────────────────────────────
                .sessionManagement(session -> session
                        .maximumSessions(2)
                        .maxSessionsPreventsLogin(false)
                )
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**"))
                )
                .userDetailsService(usuarioDetailsService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
