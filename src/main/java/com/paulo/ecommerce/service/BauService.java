package com.paulo.ecommerce.service;

import com.paulo.ecommerce.dto.CheckoutForm;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BauService {
  private final BauItemRepository itens;
  private final ProdutoRepository produtos;
  private final UsuarioRepository usuarios;
  private final PedidoRepository pedidos;
  private final PedidoMovimentacaoRepository movimentos;
  private final NotificacaoService notificacoes;

  public BauService(BauItemRepository itens, ProdutoRepository produtos, UsuarioRepository usuarios,
      PedidoRepository pedidos, PedidoMovimentacaoRepository movimentos, NotificacaoService notificacoes) {
    this.itens = itens;
    this.produtos = produtos;
    this.usuarios = usuarios;
    this.pedidos = pedidos;
    this.movimentos = movimentos;
    this.notificacoes = notificacoes;
  }

  public boolean existe(Long usuario, String origem) {
    return itens.findByUsuarioIdAndOrigem(usuario, origem).isPresent();
  }

  @Transactional
  public BauItem conceder(Long usuario, String origem, String titulo, Produto produto) {
    usuarios.findLockedById(usuario).orElseThrow();
    var anterior = itens.findByUsuarioIdAndOrigem(usuario, origem);
    if (anterior.isPresent()) return anterior.get();
    if (produto != null) {
      produto = produtos.findLockedById(produto.getId()).orElseThrow();
      if (produto.getTipo() != Produto.Tipo.FISICO || produto.getEstoque() < 1)
        throw new IllegalStateException("Prêmio sem estoque disponível para reserva.");
      produto.setEstoque(produto.getEstoque() - 1);
    }
    var item = new BauItem();
    item.usuarioId = usuario;
    item.origem = origem;
    item.titulo = titulo;
    item.produto = produto;
    return itens.save(item);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listar(String email) {
    var usuario = usuarios.findByEmail(email).orElseThrow();
    return itens.findByUsuarioIdOrderByIdDesc(usuario.getId()).stream().map(item -> {
      var dados = new LinkedHashMap<String, Object>();
      dados.put("id", item.id);
      dados.put("titulo", item.titulo);
      dados.put("origem", item.origem);
      dados.put("fisico", item.produto != null);
      dados.put("imagem", item.produto == null ? null : item.produto.getImagemUrl());
      dados.put("pedidoId", item.pedido == null ? null : item.pedido.getId());
      dados.put("estado", item.pedido == null ? "NO_BAU" : item.pedido.getStatus().name());
      dados.put("podeSolicitar", item.produto != null
          && (item.pedido == null || item.pedido.getStatus() == Pedido.Status.CANCELADO));
      return (Map<String, Object>) dados;
    }).toList();
  }

  @Transactional
  public Long solicitar(String email, Long id, CheckoutForm endereco) {
    var usuario = usuarios.findLockedByEmail(email).orElseThrow();
    var item = itens.findLockedById(id).orElseThrow();
    if (!usuario.getId().equals(item.usuarioId))
      throw new org.springframework.security.access.AccessDeniedException("Recompensa pertence a outra conta.");
    if (item.produto == null) throw new IllegalArgumentException("Este item não exige entrega.");
    if (item.pedido != null && item.pedido.getStatus() != Pedido.Status.CANCELADO)
      return item.pedido.getId();
    var pedido = new Pedido();
    pedido.setUsuario(usuario);
    pedido.setRecompensa(true);
    pedido.setNumero("RC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
    pedido.setStatus(Pedido.Status.CONFIRMADO);
    pedido.setSubtotal(BigDecimal.ZERO);
    pedido.setTotal(BigDecimal.ZERO);
    pedido.setFrete(BigDecimal.ZERO);
    pedido.setNomeDestinatario(endereco.getNomeDestinatario());
    pedido.setTelefoneContato(endereco.getTelefoneContato());
    pedido.setCepEntrega(endereco.getCep());
    pedido.setCidadeEntrega(endereco.getCidade());
    pedido.setEstadoEntrega(endereco.getUf().toUpperCase(Locale.ROOT));
    pedido.setComplementoEntrega(endereco.getComplemento());
    pedido.setEnderecoEntrega(endereco.getEnderecoCompleto());
    pedido.setObservacoes("Recompensa do Baú #" + item.id + " · " + item.origem);
    var linha = new ItemPedido();
    linha.setPedido(pedido);
    linha.setProduto(item.produto);
    linha.setQuantidade(1);
    linha.setPrecoUnitario(BigDecimal.ZERO);
    pedido.setItens(new ArrayList<>(List.of(linha)));
    pedidos.save(pedido);
    item.pedido = pedido;
    movimentos.save(new PedidoMovimentacao(pedido.getId(), pedido.getStatus(), "Entrega solicitada pelo Baú"));
    notificacoes.registrar(usuario.getId(), "Entrega de recompensa solicitada",
        item.titulo, "/pedidos/" + pedido.getId());
    return pedido.getId();
  }
}
