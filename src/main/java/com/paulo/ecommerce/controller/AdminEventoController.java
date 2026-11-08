package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.service.EventoSemanalService;
import com.paulo.ecommerce.repository.PasseTemporadaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/eventos")
public class AdminEventoController {
  private final EventoSemanalService eventos;
  private final PasseTemporadaRepository temporadas;

  public AdminEventoController(EventoSemanalService eventos, PasseTemporadaRepository temporadas) {
    this.eventos = eventos;
    this.temporadas = temporadas;
  }

  public record Rascunho(String titulo, String descricao, LocalDateTime inicio, LocalDateTime fim,
      int multiplicador, int limite, int meta, BigDecimal minimo, String selo) {}

  @GetMapping
  public String listar(Model model, @RequestParam(defaultValue = "false") boolean ativos) {
    var inicio = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
    model.addAttribute("rascunho", new Rascunho("Semana XP em Dobro",
        "Ganhe XP em dobro em compras aprovadas durante a campanha.",
        inicio, inicio.plusDays(7), 2, 200, 10, BigDecimal.ZERO, "Veterano da Semana LEVEL"));
    return pagina(model, ativos);
  }

  @PostMapping
  public String publicar(@RequestParam String titulo, @RequestParam String descricao,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
      @RequestParam int multiplicador, @RequestParam int limite, @RequestParam int meta,
      @RequestParam BigDecimal minimo, @RequestParam String selo,
      @RequestParam(defaultValue = "publicar") String acao, Model model, RedirectAttributes flash) {
    try {
      if ("prever".equals(acao)) {
        var previa = eventos.preparar(titulo, descricao, inicio, fim, multiplicador, limite, meta, minimo, selo);
        model.addAttribute("previa", previa);
      } else {
        eventos.publicar(titulo, descricao, inicio, fim, multiplicador, limite, meta, minimo, selo);
        flash.addFlashAttribute("msg", "Evento publicado.");
        return "redirect:/admin/eventos";
      }
    } catch (IllegalArgumentException error) {
      model.addAttribute("erro", error.getMessage());
    }
    model.addAttribute("rascunho", new Rascunho(titulo, descricao, inicio, fim, multiplicador, limite, meta, minimo, selo));
    return pagina(model, false);
  }

  private String pagina(Model model, boolean ativos) {
    YearMonth mes = YearMonth.now();
    model.addAttribute("temporada", temporadas.findByAnoAndMes(mes.getYear(), mes.getMonthValue()).orElse(null));
    model.addAttribute("eventos", eventos.listar(null).stream()
        .filter(evento -> !ativos || evento.estado().equals("Em andamento"))
        .toList());
    model.addAttribute("ativos", ativos);
    return "admin/eventos";
  }
}
