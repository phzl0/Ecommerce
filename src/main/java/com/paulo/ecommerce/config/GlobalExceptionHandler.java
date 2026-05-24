package com.paulo.ecommerce.config;

import com.paulo.ecommerce.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNaoEncontrado(RecursoNaoEncontradoException ex, Model model) {
        model.addAttribute("codigo", 404);
        model.addAttribute("mensagem", ex.getMessage());
        return "error/erro";
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public String handleEstoque(EstoqueInsuficienteException ex, Model model) {
        model.addAttribute("codigo", 400);
        model.addAttribute("mensagem", ex.getMessage());
        return "error/erro";
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public String handleEmail(EmailJaCadastradoException ex, Model model) {
        model.addAttribute("codigo", 400);
        model.addAttribute("mensagem", ex.getMessage());
        return "error/erro";
    }

    @ExceptionHandler(CarrinhoVazioException.class)
    public String handleCarrinho(CarrinhoVazioException ex, Model model) {
        model.addAttribute("codigo", 400);
        model.addAttribute("mensagem", ex.getMessage());
        return "error/erro";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handle404(Model model) {
        model.addAttribute("codigo", 404);
        model.addAttribute("mensagem", "Página não encontrada.");
        return "error/erro";
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handle403(Model model) {
        model.addAttribute("codigo", 403);
        model.addAttribute("mensagem", "Você não tem permissão para acessar esta página.");
        return "error/erro";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeral(Exception ex, Model model) {
        log.error("Erro interno ao processar requisicao", ex);
        model.addAttribute("codigo", 500);
        model.addAttribute("mensagem", "Ocorreu um erro interno. Tente novamente.");
        return "error/erro";
    }
}
