package com.paulo.ecommerce.service;

import com.paulo.ecommerce.dto.api.CheckoutItemRequest;
import com.paulo.ecommerce.dto.CarrinhoItem;
import com.paulo.ecommerce.dto.CheckoutForm;
import com.paulo.ecommerce.exception.CarrinhoVazioException;
import com.paulo.ecommerce.exception.EstoqueInsuficienteException;
import com.paulo.ecommerce.exception.RecursoNaoEncontradoException;
import com.paulo.ecommerce.model.ItemPedido;
import com.paulo.ecommerce.model.Pedido;
import com.paulo.ecommerce.model.Produto;
import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.repository.PedidoRepository;
import com.paulo.ecommerce.repository.ProdutoRepository;
import com.paulo.ecommerce.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarrinhoService carrinhoService;
    private final EmailService emailService;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProdutoRepository produtoRepository,
                         UsuarioRepository usuarioRepository,
                         CarrinhoService carrinhoService,
                         EmailService emailService) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
        this.carrinhoService = carrinhoService;
        this.emailService = emailService;
    }

    @Transactional
    public Pedido finalizarPedido(String emailUsuario,
                                  CheckoutForm checkoutForm,
                                  HttpSession session) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Usuario", 0L));

        List<CarrinhoItem> itensCarrinho = carrinhoService.getCarrinho(session);

        Pedido pedido = criarPedido(usuario, checkoutForm, itensCarrinho);
        carrinhoService.limpar(session);
        return pedido;
    }

    @Transactional
    public Pedido finalizarPedidoApi(String emailUsuario,
                                     CheckoutForm checkoutForm,
                                     java.util.List<CheckoutItemRequest> itensRequest) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Usuario", 0L));

        List<CarrinhoItem> itensCarrinho = itensRequest.stream().sorted(java.util.Comparator.comparing(CheckoutItemRequest::produtoId))
                .map(item -> {
                    Produto produto = produtoRepository.findById(item.produtoId())
                            .orElseThrow(() ->
                                    new RecursoNaoEncontradoException("Produto", item.produtoId()));
                    return new CarrinhoItem(
                            produto.getId(),
                            produto.getNome(),
                            produto.getImagemUrl(),
                            produto.getPreco(),
                            item.quantidade()
                    );
                })
                .toList();

        return criarPedido(usuario, checkoutForm, itensCarrinho);
    }

    private Pedido criarPedido(Usuario usuario,
                               CheckoutForm checkoutForm,
                               List<CarrinhoItem> itensCarrinho) {

        if (itensCarrinho.isEmpty()) {
            throw new CarrinhoVazioException();
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setNumero(gerarNumeroPedido(usuario));
        pedido.setNomeDestinatario(checkoutForm.getNomeDestinatario());
        pedido.setTelefoneContato(checkoutForm.getTelefoneContato());
        pedido.setCepEntrega(checkoutForm.getCep());
        pedido.setCidadeEntrega(checkoutForm.getCidade());
        pedido.setEstadoEntrega(checkoutForm.getUf().toUpperCase(Locale.ROOT));
        pedido.setComplementoEntrega(checkoutForm.getComplemento());
        pedido.setObservacoes(checkoutForm.getObservacoes());
        pedido.setEnderecoEntrega(checkoutForm.getEnderecoCompleto());
        java.math.BigDecimal subtotal = itensCarrinho.stream()
                .map(CarrinhoItem::getSubtotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal frete = calcularFrete(subtotal);
        pedido.setSubtotal(subtotal);
        pedido.setFrete(frete);
        pedido.setDesconto(java.math.BigDecimal.ZERO);
        pedido.setTotal(subtotal.add(frete));

        List<ItemPedido> itens = new ArrayList<>();
        for (CarrinhoItem ci : itensCarrinho) {
            Produto produto = produtoRepository.findLockedById(ci.getProdutoId())
                    .orElseThrow(() ->
                            new RecursoNaoEncontradoException("Produto",
                                    ci.getProdutoId()));

            if (!produto.isAtivo() || ci.getQuantidade() <= 0 || produto.getEstoque() < ci.getQuantidade()) {
                throw new EstoqueInsuficienteException(produto.getNome());
            }

            produto.setEstoque(produto.getEstoque() - ci.getQuantidade());
            produtoRepository.save(produto);

            ItemPedido item = new ItemPedido();
            item.setPedido(pedido);
            item.setProduto(produto);
            item.setQuantidade(ci.getQuantidade());
            item.setPrecoUnitario(produto.getPreco());
            itens.add(item);
        }

        subtotal = itens.stream().map(i -> i.getPrecoUnitario().multiply(java.math.BigDecimal.valueOf(i.getQuantidade()))).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        pedido.setSubtotal(subtotal);
        pedido.setFrete(calcularFrete(subtotal));
        pedido.setTotal(subtotal.add(pedido.getFrete()));
        pedido.setItens(itens);
        Pedido salvo = pedidoRepository.save(pedido);
        emailService.enviarConfirmacaoPedido(salvo);
        return salvo;
    }

    public List<Pedido> listarPorUsuario(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Usuario", 0L));
        return pedidoRepository
                .findByUsuarioIdOrderByDataPedidoDesc(usuario.getId());
    }

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll(
                Sort.by("dataPedido").descending());
    }

    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findByIdComDetalhes(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Pedido", id));
    }

    public Pedido buscarPorIdDoUsuario(Long id, String emailUsuario) {
        Pedido pedido = buscarPorId(id);
        if (!pedido.getUsuario().getEmail().equalsIgnoreCase(emailUsuario)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Pedido nao pertence ao usuario autenticado."
            );
        }
        return pedido;
    }

    @Transactional
    public void atualizarStatus(Long id, Pedido.Status status) {
        Pedido pedido = pedidoRepository.findLockedById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
        if (pedido.getStatus() == status) return;
        if (pedido.getStatus() == Pedido.Status.CANCELADO || (status == Pedido.Status.CONFIRMADO && pedido.getStatus() != Pedido.Status.PENDENTE)) throw new IllegalStateException("Transição de pedido inválida.");
        if (status == Pedido.Status.ENVIADO && pedido.getStatus() != Pedido.Status.CONFIRMADO) throw new IllegalStateException("Confirme o pagamento antes do envio.");
        if (status == Pedido.Status.ENTREGUE && pedido.getStatus() != Pedido.Status.ENVIADO) throw new IllegalStateException("O pedido ainda não foi enviado.");
        if (status == Pedido.Status.CANCELADO) devolverEstoque(pedido);
        pedido.setStatus(status);
        pedidoRepository.save(pedido);
        emailService.enviarAtualizacaoStatus(pedido);
    }

    @Transactional
    public void cancelarPedido(Long id, String emailUsuario) {
        Pedido pedido = pedidoRepository.findLockedById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));

        if (!pedido.getUsuario().getEmail().equals(emailUsuario)) {
            throw new org.springframework.security.access
                    .AccessDeniedException("Pedido nao pertence ao usuario.");
        }

        if (pedido.getStatus() != Pedido.Status.PENDENTE) {
            throw new RuntimeException(
                    "Apenas pedidos pendentes podem ser cancelados.");
        }

        devolverEstoque(pedido);

        pedido.setStatus(Pedido.Status.CANCELADO);
        pedidoRepository.save(pedido);
    }

    private void devolverEstoque(Pedido pedido) {
        for (ItemPedido item : pedido.getItens().stream().sorted(java.util.Comparator.comparing(i -> i.getProduto().getId())).toList()) {
            Produto produto = produtoRepository.findLockedById(item.getProduto().getId()).orElseThrow();
            produto.setEstoque(produto.getEstoque() + item.getQuantidade());
            produtoRepository.save(produto);
        }
    }

    private String gerarNumeroPedido(Usuario usuario) {
        String baseUsuario = usuario.getNome() == null ? "CLI" : usuario.getNome()
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        if (baseUsuario.isBlank()) {
            baseUsuario = "CLI";
        }
        baseUsuario = baseUsuario.substring(0, Math.min(baseUsuario.length(), 3));
        return "ML-" + baseUsuario + "-" + System.currentTimeMillis();
    }

    private java.math.BigDecimal calcularFrete(java.math.BigDecimal subtotal) {
        if (subtotal.compareTo(new java.math.BigDecimal("299.00")) >= 0) {
            return java.math.BigDecimal.ZERO;
        }
        if (subtotal.compareTo(new java.math.BigDecimal("149.00")) >= 0) {
            return new java.math.BigDecimal("14.90");
        }
        return new java.math.BigDecimal("24.90");
    }
}
