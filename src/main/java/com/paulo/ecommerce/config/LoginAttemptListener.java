package com.paulo.ecommerce.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.*;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptListener {

  private final RateLimitConfig rateLimitConfig;
  private final HttpServletRequest request;

  public LoginAttemptListener(RateLimitConfig rateLimitConfig, HttpServletRequest request) {
    this.rateLimitConfig = rateLimitConfig;
    this.request = request;
  }

  @EventListener
  public void onSucesso(AuthenticationSuccessEvent event) {
    rateLimitConfig.resetar(getIp());
  }

  @EventListener
  public void onFalha(AbstractAuthenticationFailureEvent event) {
    rateLimitConfig.registrarTentativaFalha(getIp());
  }

  private String getIp() {
    return request.getRemoteAddr();
  }
}
