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

    @Cacheable("metricas")
    public long totalPedidos() {
        return pedidoRepository.count();
    }

    @Cacheable("metricas")
    public long totalProdutos() {
        return produtoRepository.count();
    }

    @Cacheable("metricas")
    public long totalClientes() {
        return usuarioRepository.count();
    }

    // Substitua os métodos de métricas

    @Cacheable(value = "metricas", key = "'receita-total'")
    public BigDecimal receitaTotal() {
        return pedidoRepository.receitaDesde(
                LocalDateTime.of(2000, 1, 1, 0, 0));
    }

    @Cacheable(value = "metricas", key = "'pedidos-recentes'")
    public List<Pedido> pedidosRecentes() {
        return pedidoRepository.findPedidosRecentes(
                LocalDateTime.now().minusDays(30));
    }

    @Cacheable(value = "metricas", key = "'sem-estoque'")
    public long produtosSemEstoque() {
        return produtoRepository.countSemEstoque();
    }

    @Cacheable(value = "metricas", key = "'pendentes'")
    public long pedidosPendentes() {
        return pedidoRepository.countByStatus(Pedido.Status.PENDENTE);
    }
}