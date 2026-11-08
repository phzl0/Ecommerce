package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.Pedido;
import com.paulo.ecommerce.model.Produto;
import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

  private final PedidoRepository pedidoRepository;
  private final ProdutoRepository produtoRepository;
  private final UsuarioRepository usuarioRepository;

  public AdminService(
      PedidoRepository pedidoRepository,
      ProdutoRepository produtoRepository,
      UsuarioRepository usuarioRepository) {
    this.pedidoRepository = pedidoRepository;
    this.produtoRepository = produtoRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public long totalPedidos() {
    return pedidoRepository.count();
  }

  public long totalProdutos() {
    return produtoRepository.countByAtivoTrue();
  }

  public long totalClientes() {
    return usuarioRepository.countByRole(Usuario.Role.CLIENTE);
  }

  public BigDecimal receitaTotal() {
    return pedidoRepository.receitaDesde(LocalDateTime.of(2000, 1, 1, 0, 0));
  }

  public List<Pedido> pedidosRecentes() {
    return pedidoRepository.findPedidosRecentes(LocalDateTime.now().minusDays(30), PageRequest.of(0, 8));
  }

  public long produtosSemEstoque() {
    return produtoRepository.countSemEstoque();
  }

  public long pedidosPendentes() {
    return pedidoRepository.countByStatus(Pedido.Status.PENDENTE);
  }
  public long pedidosParaEnvio() {
    return pedidoRepository.countByStatusAndTemporadaPasseIdIsNull(Pedido.Status.CONFIRMADO);
  }

  public Page<Pedido> pedidos(Pedido.Status status, String busca, boolean envio, int pagina) {
    return pedidoRepository.buscarPedidosAdmin(status, busca, envio,
        PageRequest.of(Math.max(0, pagina), 20, Sort.by(Sort.Direction.DESC, "dataPedido", "id")));
  }

  public Page<Produto> produtos(String busca, boolean semEstoque, int pagina) {
    return produtoRepository.buscarCatalogoAdmin(busca, semEstoque,
        PageRequest.of(Math.max(0, pagina), 20, Sort.by("nome", "id")));
  }
}
