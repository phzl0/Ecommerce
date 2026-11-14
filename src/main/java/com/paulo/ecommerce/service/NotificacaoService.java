package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.Notificacao;
import com.paulo.ecommerce.repository.NotificacaoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoService {
  private final NotificacaoRepository repository;

  public NotificacaoService(NotificacaoRepository repository) {
    this.repository = repository;
  }

  public void registrar(Long usuarioId, String titulo, String mensagem, String destino) {
    var item = new Notificacao();
    item.usuarioId = usuarioId;
    item.titulo = titulo;
    item.mensagem = mensagem;
    item.destino = destino;
    item.criadoEm = LocalDateTime.now();
    repository.save(item);
  }

  public record Item(Long id, String titulo, String mensagem, String destino, LocalDateTime criadoEm, boolean lida) {}
  public record Caixa(long naoLidas, List<Item> itens) {}

  @Transactional(readOnly = true)
  public Caixa caixa(Long usuarioId) {
    return new Caixa(repository.countByUsuarioIdAndLidaFalse(usuarioId),
        repository.findTop30ByUsuarioIdOrderByIdDesc(usuarioId).stream()
            .map(n -> new Item(n.id, n.titulo, n.mensagem, n.destino, n.criadoEm, n.lida))
            .toList());
  }

  @Transactional
  public void marcarLidas(Long usuarioId, Long ate) {
    repository.marcarLidas(usuarioId, ate);
  }
}
