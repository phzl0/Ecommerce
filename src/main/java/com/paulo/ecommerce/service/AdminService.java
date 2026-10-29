package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.Pedido;
import com.paulo.ecommerce.repository.*;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {

    private final PedidoRepository  pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    public AdminService(PedidoRepository pedidoRepository,
                        ProdutoRepository produtoRepository,
                        UsuarioRepository usuarioRepository) {
        this.pedidoRepository  = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public long totalPedidos() {
        return pedidoRepository.count();
    }

    public long totalProdutos() {
        return produtoRepository.count();
    }

    public long totalClientes() {
        return usuarioRepository.count();
    }

    // Substitua os métodos de métricas

    public BigDecimal receitaTotal() {
        return pedidoRepository.receitaDesde(
                LocalDateTime.of(2000, 1, 1, 0, 0));
    }

    public List<Pedido> pedidosRecentes() {
        return pedidoRepository.findPedidosRecentes(
                LocalDateTime.now().minusDays(30));
    }

    public long produtosSemEstoque() {
        return produtoRepository.countSemEstoque();
    }

    public long pedidosPendentes() {
        return pedidoRepository.countByStatus(Pedido.Status.PENDENTE);
    }
}
