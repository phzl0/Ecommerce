package com.paulo.ecommerce.service;

import com.paulo.ecommerce.exception.RecursoNaoEncontradoException;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvaliacaoService {

  @org.springframework.beans.factory.annotation.Autowired private PedidoRepository pedidos;
  private final AvaliacaoRepository avaliacaoRepository;
  private final ProdutoRepository produtoRepository;
  private final UsuarioRepository usuarioRepository;
  private final PasseService passeService;

  public AvaliacaoService(
      AvaliacaoRepository avaliacaoRepository,
      ProdutoRepository produtoRepository,
      UsuarioRepository usuarioRepository,
      PasseService passeService) {
    this.avaliacaoRepository = avaliacaoRepository;
    this.produtoRepository = produtoRepository;
    this.usuarioRepository = usuarioRepository;
    this.passeService = passeService;
  }

  @Transactional
  public Avaliacao avaliar(Long produtoId, String emailUsuario, Integer nota, String comentario) {
    Usuario usuario =
        usuarioRepository
            .findByEmail(emailUsuario)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", 0L));

    Produto produto =
        produtoRepository
            .findById(produtoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));

    if (nota == null || nota < 1 || nota > 5 || (comentario != null && comentario.length() > 500))
      throw new IllegalArgumentException("Avaliação inválida.");
    boolean comprado =
        pedidos.findByUsuarioIdOrderByDataPedidoDesc(usuario.getId()).stream()
            .anyMatch(
                p ->
                    p.getStatus() != Pedido.Status.CANCELADO
                        && p.getPagamento() != null
                        && p.getPagamento().getStatus() == Pagamento.StatusPagamento.APROVADO
                        && p.getItens().stream()
                            .anyMatch(i -> i.getProduto().getId().equals(produtoId)));
    if (!comprado)
      throw new IllegalArgumentException(
          "Avaliações são permitidas para produtos de compras aprovadas desta conta.");
    boolean primeira =
        !avaliacaoRepository.existsByUsuarioIdAndProdutoId(usuario.getId(), produtoId);
    // Atualiza se já avaliou, cria se não
    Avaliacao avaliacao =
        avaliacaoRepository
            .findByUsuarioIdAndProdutoId(usuario.getId(), produtoId)
            .orElse(new Avaliacao());

    avaliacao.setUsuario(usuario);
    avaliacao.setProduto(produto);
    avaliacao.setNota(nota);
    avaliacao.setComentario(comentario);

    Avaliacao salva = avaliacaoRepository.save(avaliacao);
    if (primeira)
      passeService.registrarAcao(
          emailUsuario, PasseAcaoTipo.REVIEW_SUBMITTED, 1, "review-" + produtoId);
    return salva;
  }

  public List<Avaliacao> listarPorProduto(Long produtoId) {
    return avaliacaoRepository.findByProdutoIdOrderByCriadoEmDesc(produtoId);
  }

  public Double mediaPorProduto(Long produtoId) {
    Double media = avaliacaoRepository.mediaNotasPorProduto(produtoId);
    return media != null ? Math.round(media * 10.0) / 10.0 : 0.0;
  }

  public long totalAvaliacoes(Long produtoId) {
    return avaliacaoRepository.countByProdutoId(produtoId);
  }

  public boolean usuarioJaAvaliou(Long produtoId, String emailUsuario) {
    return usuarioRepository
        .findByEmail(emailUsuario)
        .map(u -> avaliacaoRepository.existsByUsuarioIdAndProdutoId(u.getId(), produtoId))
        .orElse(false);
  }
}
