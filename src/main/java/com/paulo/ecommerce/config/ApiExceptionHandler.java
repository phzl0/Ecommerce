package com.paulo.ecommerce.config;

import com.paulo.ecommerce.exception.*;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(annotations = RestController.class)
@Order(0)
public class ApiExceptionHandler {
  @ExceptionHandler({
    IllegalArgumentException.class,
    IllegalStateException.class,
    EstoqueInsuficienteException.class,
    CarrinhoVazioException.class
  })
  public ResponseEntity<Map<String, String>> invalid(
      RuntimeException error, jakarta.servlet.http.HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "message",
                error.getMessage() == null ? "Solicitação inválida." : error.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException error) {
    return ResponseEntity.badRequest().body(Map.of("message", "Confira os campos informados."));
  }
}
