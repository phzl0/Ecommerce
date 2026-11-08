package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.EventoResgate;
import com.paulo.ecommerce.model.EventoSemanal;
import com.paulo.ecommerce.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoSemanalService {
  private final EventoSemanalRepository eventos;
  private final EventoResgateRepository resgates;
  private final PedidoRepository pedidos;
  private final UsuarioRepository usuarios;
  private final NotificacaoService notificacoes;

  public EventoSemanalService(EventoSemanalRepository eventos, EventoResgateRepository resgates,
      PedidoRepository pedidos, UsuarioRepository usuarios, NotificacaoService notificacoes) {
    this.eventos = eventos;
    this.resgates = resgates;
    this.pedidos = pedidos;
    this.usuarios = usuarios;
    this.notificacoes = notificacoes;
  }

  public record Visao(Long id, String titulo, String descricao, LocalDateTime inicio,
      LocalDateTime fim, int multiplicador, int limiteXp, int metaXp, BigDecimal minimoCompra,
      String selo, String estado, int xp, boolean resgatado, boolean podeResgatar) {}

  public List<Visao> listar(Long usuarioId) {
    LocalDateTime agora = LocalDateTime.now();
    return eventos.findTop30ByOrderByInicioDesc().stream().map(evento -> {
      int xp = usuarioId == null ? 0 : xp(evento, usuarioId);
      boolean resgatado = usuarioId != null && resgates.existsByEventoIdAndUsuarioId(evento.id, usuarioId);
      String estado = agora.isBefore(evento.inicio) ? "Agendado"
          : agora.isBefore(evento.fim) ? "Em andamento" : "Encerrado";
      return new Visao(evento.id, evento.titulo, evento.descricao, evento.inicio, evento.fim,
          evento.multiplicador, evento.limiteXp, evento.metaXp, evento.minimoCompra, evento.selo,
          estado, xp, resgatado, !resgatado && xp >= evento.metaXp && agora.isBefore(evento.fim.plusDays(7)));
    }).toList();
  }

  @Transactional
  public void publicar(String titulo, String descricao, LocalDateTime inicio, LocalDateTime fim,
      int multiplicador, int limite, int meta, BigDecimal minimo, String selo) {
    eventos.save(preparar(titulo, descricao, inicio, fim, multiplicador, limite, meta, minimo, selo));
  }

  public EventoSemanal preparar(String titulo, String descricao, LocalDateTime inicio, LocalDateTime fim,
      int multiplicador, int limite, int meta, BigDecimal minimo, String selo) {
    titulo = titulo.trim();
    descricao = descricao.trim();
    selo = selo.trim();
    if (titulo.isEmpty() || titulo.length() > 100 || descricao.isEmpty() || descricao.length() > 500
        || selo.isEmpty() || selo.length() > 80)
      throw new IllegalArgumentException("Confira o título, a descrição e o nome do selo.");
    LocalDateTime agora = LocalDateTime.now();
    if (inicio.isBefore(agora.minusMinutes(15)) || !fim.isAfter(agora)
        || !fim.isAfter(inicio) || fim.isAfter(inicio.plusDays(7)))
      throw new IllegalArgumentException("Defina um período futuro ou com início agora, de até sete dias.");
    if (multiplicador < 1 || multiplicador > 5 || meta < 1 || limite < meta || limite > 100_000
        || minimo.signum() < 0 || minimo.compareTo(new BigDecimal("999999.99")) > 0
        || minimo.stripTrailingZeros().scale() > 2)
      throw new IllegalArgumentException("Confira o multiplicador, a meta, o limite de XP e o valor mínimo.");

    var evento = new EventoSemanal();
    evento.titulo = titulo;
    evento.descricao = descricao;
    evento.inicio = inicio.isBefore(agora) ? agora : inicio;
    evento.fim = fim;
    evento.multiplicador = multiplicador;
    evento.limiteXp = limite;
    evento.metaXp = meta;
    evento.minimoCompra = minimo;
    evento.selo = selo;
    return evento;
  }

  @Transactional
  public void resgatar(Long id, String email) {
    var usuario = usuarios.findLockedByEmail(email).orElseThrow();
    var evento = eventos.findById(id).orElseThrow(() -> new IllegalArgumentException("Evento indisponível."));
    if (resgates.existsByEventoIdAndUsuarioId(id, usuario.getId())) return;
    if (!LocalDateTime.now().isBefore(evento.fim.plusDays(7)) || xp(evento, usuario.getId()) < evento.metaXp)
      throw new IllegalStateException("O selo ainda não está disponível para resgate.");

    var resgate = new EventoResgate();
    resgate.eventoId = id;
    resgate.usuarioId = usuario.getId();
    resgate.resgatadoEm = LocalDateTime.now();
    resgates.save(resgate);
    notificacoes.registrar(usuario.getId(), "Selo resgatado", evento.selo, "/eventos");
  }

  public List<String> selos(Long usuarioId) {
    return resgates.findByUsuarioIdOrderByResgatadoEmDesc(usuarioId).stream()
        .map(resgate -> eventos.findById(resgate.eventoId).orElseThrow())
        .filter(evento -> xp(evento, usuarioId) >= evento.metaXp)
        .map(evento -> evento.selo).toList();
  }

  public void notificarMeta(Long usuarioId, LocalDateTime aprovadoEm, BigDecimal valor) {
    for (var evento : eventos.findByInicioLessThanEqualAndFimGreaterThan(aprovadoEm, aprovadoEm)) {
      if (valor.compareTo(evento.minimoCompra) < 0) continue;
      long pontos = pontos(evento, valor);
      long total = xpBruto(evento, usuarioId);
      if (total >= evento.metaXp && total - pontos < evento.metaXp
          && !resgates.existsByEventoIdAndUsuarioId(evento.id, usuarioId)) {
        notificacoes.registrar(usuarioId, "Recompensa de evento disponível", evento.selo, "/eventos");
      }
    }
  }

  private int xp(EventoSemanal evento, Long usuarioId) {
    return (int) Math.min(evento.limiteXp, xpBruto(evento, usuarioId));
  }

  private long xpBruto(EventoSemanal evento, Long usuarioId) {
    return pedidos.comprasEvento(usuarioId, evento.inicio, evento.fim, evento.minimoCompra)
        .stream().mapToLong(valor -> pontos(evento, valor)).sum();
  }

  private long pontos(EventoSemanal evento, BigDecimal valor) {
    return valor.divideToIntegralValue(BigDecimal.TEN).longValue() * evento.multiplicador;
  }
}
