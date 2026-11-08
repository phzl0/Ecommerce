package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.service.EventoSemanalService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/eventos")
public class AdminEventoController {
  private final EventoSemanalService eventos;

  public AdminEventoController(EventoSemanalService eventos) {
    this.eventos = eventos;
  }

  @GetMapping
  public String listar(Model model) {
    var inicio = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
    model.addAttribute("inicio", inicio.toString());
    model.addAttribute("fim", inicio.plusDays(7).toString());
    model.addAttribute("eventos", eventos.listar(null));
    return "admin/eventos";
  }

  @PostMapping
  public String publicar(@RequestParam String titulo, @RequestParam String descricao,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
      @RequestParam int multiplicador, @RequestParam int limite, @RequestParam int meta,
      @RequestParam BigDecimal minimo, @RequestParam String selo, RedirectAttributes flash) {
    try {
      eventos.publicar(titulo, descricao, inicio, fim, multiplicador, limite, meta, minimo, selo);
      flash.addFlashAttribute("msg", "Evento publicado.");
    } catch (IllegalArgumentException error) {
      flash.addFlashAttribute("erro", error.getMessage());
    }
    return "redirect:/admin/eventos";
  }
}
