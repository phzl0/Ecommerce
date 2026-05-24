package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.service.AvaliacaoService;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @PostMapping("/produto/{produtoId}")
    public String avaliar(@PathVariable Long produtoId,
                          @RequestParam @Min(1) @Max(5) Integer nota,
                          @RequestParam(required = false) String comentario,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        try {
            avaliacaoService.avaliar(
                    produtoId, userDetails.getUsername(), nota, comentario);
            redirectAttributes.addFlashAttribute("msg",
                    "Avaliação enviada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro",
                    "Erro ao enviar avaliação.");
        }
        return "redirect:/produtos/" + produtoId;
    }
}