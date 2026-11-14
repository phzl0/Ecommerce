package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FidelidadeService {
  private final FidelidadeConfigRepository configuracoes;
  private final VipContaRepository contas;
  private final VipMovimentoRepository movimentos;
  private final VipNivelRepository niveis;
  private final UsuarioRepository usuarios;
  private final BauService bau;

  public FidelidadeService(FidelidadeConfigRepository configuracoes, VipContaRepository contas,
      VipMovimentoRepository movimentos, VipNivelRepository niveis, UsuarioRepository usuarios, BauService bau) {
    this.configuracoes = configuracoes;
    this.contas = contas;
    this.movimentos = movimentos;
    this.niveis = niveis;
    this.usuarios = usuarios;
    this.bau = bau;
  }

  @Transactional
  public FidelidadeConfig config() {
    return configuracoes.findById(1L).orElseGet(() -> configuracoes.save(new FidelidadeConfig()));
  }

  private VipConta conta(Long usuarioId) {
    var conta = contas.findById(usuarioId).orElseGet(() -> {
      var nova = new VipConta();
      nova.usuarioId = usuarioId;
      return contas.save(nova);
    });
    if (conta.ultimaAtividade != null && !conta.expirado
        && !conta.ultimaAtividade.plusDays(config().diasInatividade).isAfter(LocalDateTime.now())) {
      conta.cicloInicio = LocalDateTime.now();
      conta.expirado = true;
    }
    return conta;
  }

  @Transactional
  public void creditar(Long usuarioId, String referencia, long pontos, Long pedidoId) {
    usuarios.findLockedById(usuarioId).orElseThrow();
    var conta = conta(usuarioId);
    if (movimentos.existsByUsuarioIdAndReferencia(usuarioId, referencia)) return;
    if (pontos <= 0) return;
    var movimento = new VipMovimento();
    movimento.usuarioId = usuarioId;
    movimento.referencia = referencia;
    movimento.pontos = pontos;
    movimento.pedidoId = pedidoId;
    movimentos.save(movimento);
    conta.ultimaAtividade = movimento.criadoEm;
    conta.expirado = false;
  }

  @Transactional
  public void compra(Pedido pedido) {
    if (pedido.isRecompensa()) return;
    long pontos = pedido.getSubtotal().subtract(pedido.getDesconto())
        .multiply(java.math.BigDecimal.valueOf(config().pontosPorReal)).longValue();
    creditar(pedido.getUsuario().getId(), "COMPRA-" + pedido.getId(), pontos, pedido.getId());
    if (pedido.getTemporadaPasseId() != null) {
      creditar(pedido.getUsuario().getId(), "PRIMEIRO-PASSE", config().bonusPrimeiroPasse, pedido.getId());
      creditar(pedido.getUsuario().getId(), "PASSE-" + pedido.getTemporadaPasseId(),
          config().bonusPasseTemporada, pedido.getId());
    }
  }

  @Transactional
  public void estornar(Long pedidoId) {
    var afetados = movimentos.findByPedidoId(pedidoId);
    for (var movimento : afetados) movimento.estornado = true;
    movimentos.flush();
    afetados.stream().map(m -> m.usuarioId).distinct().forEach(id -> contas.findById(id).ifPresent(c -> {
      c.ultimaAtividade = movimentos.findByUsuarioIdAndEstornadoFalseOrderByCriadoEmDesc(id).stream()
          .filter(m -> !m.criadoEm.isBefore(c.cicloInicio)).map(m -> m.criadoEm).findFirst().orElse(null);
    }));
  }

  @Transactional
  public void reconciliarMissao(Long usuarioId, String referencia, boolean completa) {
    movimentos.findByUsuarioIdAndReferencia(usuarioId, referencia).ifPresent(m -> m.estornado = !completa);
  }

  private long saldo(VipConta conta) {
    return movimentos.findByUsuarioIdAndEstornadoFalseOrderByCriadoEmDesc(conta.usuarioId).stream()
        .filter(m -> !m.criadoEm.isBefore(conta.cicloInicio)).mapToLong(m -> m.liquido()).sum();
  }

  @Transactional
  public Map<String, Object> perfil(String email) {
    var usuario = usuarios.findLockedByEmail(email).orElseThrow();
    var conta = conta(usuario.getId());
    long pontos = saldo(conta);
    var ativos = niveis.findByAtivoTrueOrderByPontosAsc();
    var nivel = ativos.stream().filter(n -> pontos >= n.pontos).reduce((a, b) -> b);
    var resposta = new LinkedHashMap<String, Object>();
    resposta.put("pontos", pontos);
    resposta.put("nivel", nivel.map(n -> n.nome).orElse("Explorador"));
    resposta.put("expirado", conta.expirado);
    resposta.put("validade", conta.ultimaAtividade == null ? null
        : conta.ultimaAtividade.plusDays(config().diasInatividade));
    resposta.put("diasInatividade", config().diasInatividade);
    resposta.put("niveis", ativos.stream().map(n -> Map.of(
        "id", n.id, "nome", n.nome, "pontos", n.pontos, "premio", n.premio,
        "fisico", n.produto != null, "disponivel", pontos >= n.pontos && !bau.existe(usuario.getId(), "VIP-" + n.id),
        "resgatado", bau.existe(usuario.getId(), "VIP-" + n.id))).toList());
    resposta.put("historico", movimentos.findByUsuarioIdAndEstornadoFalseOrderByCriadoEmDesc(usuario.getId())
        .stream().limit(20).map(m -> Map.of("origem", m.referencia, "pontos", m.liquido(), "data", m.criadoEm)).toList());
    return resposta;
  }

  @Transactional
  public Long resgatar(String email, Long id) {
    var usuario = usuarios.findLockedByEmail(email).orElseThrow();
    var nivel = niveis.findById(id).orElseThrow();
    if (!nivel.ativo || saldo(conta(usuario.getId())) < nivel.pontos)
      throw new IllegalStateException("Este nível VIP ainda está bloqueado.");
    return bau.conceder(usuario.getId(), "VIP-" + id, nivel.premio, nivel.produto).id;
  }
}
