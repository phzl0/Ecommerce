package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.dto.api.AccountOrderDetailResponse;
import com.paulo.ecommerce.dto.api.AccountOrderItemResponse;
import com.paulo.ecommerce.dto.api.AccountOrderSummaryResponse;
import com.paulo.ecommerce.dto.api.AccountProfileResponse;
import com.paulo.ecommerce.dto.api.PaymentRedirectRequest;
import com.paulo.ecommerce.dto.api.PaymentRedirectResponse;
import com.paulo.ecommerce.dto.api.UpdateProfileRequest;
import com.paulo.ecommerce.model.Pagamento;
import com.paulo.ecommerce.model.Pedido;
import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.service.PagamentoService;
import com.paulo.ecommerce.service.PedidoService;
import com.paulo.ecommerce.service.UsuarioService;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountApiController {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final UsuarioService usuarioService;
  private final PedidoService pedidoService;
  private final PagamentoService pagamentoService;

  public AccountApiController(
      UsuarioService usuarioService,
      PedidoService pedidoService,
      PagamentoService pagamentoService) {
    this.usuarioService = usuarioService;
    this.pedidoService = pedidoService;
    this.pagamentoService = pagamentoService;
  }

  @GetMapping("/profile")
  public AccountProfileResponse profile(@AuthenticationPrincipal UserDetails userDetails) {
    Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
    int totalPedidos = pedidoService.listarPorUsuario(usuario.getEmail()).size();
    return new AccountProfileResponse(
        usuario.getId(),
        usuario.getNome(),
        usuario.getEmail(),
        usuario.getRole().name(),
        totalPedidos);
  }

  @PutMapping("/profile")
  public AccountProfileResponse updateProfile(
      @RequestBody UpdateProfileRequest request, @AuthenticationPrincipal UserDetails userDetails) {
    Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
    Usuario atualizado =
        usuarioService.atualizarPerfil(usuario.getId(), request.nome(), request.senha());
    int totalPedidos = pedidoService.listarPorUsuario(atualizado.getEmail()).size();
    return new AccountProfileResponse(
        atualizado.getId(),
        atualizado.getNome(),
        atualizado.getEmail(),
        atualizado.getRole().name(),
        totalPedidos);
  }

  @GetMapping("/orders")
  public List<AccountOrderSummaryResponse> orders(
      @AuthenticationPrincipal UserDetails userDetails) {
    return pedidoService.listarPorUsuario(userDetails.getUsername()).stream()
        .map(this::toSummary)
        .toList();
  }

  @GetMapping("/orders/{id}")
  public AccountOrderDetailResponse orderDetail(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Pedido pedido = pedidoService.buscarPorIdDoUsuario(id, userDetails.getUsername());
    return toDetail(pedido);
  }

  @PostMapping("/orders/{id}/cancel")
  public AccountOrderDetailResponse cancelOrder(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    pedidoService.cancelarPedido(id, userDetails.getUsername());
    Pedido pedido = pedidoService.buscarPorIdDoUsuario(id, userDetails.getUsername());
    return toDetail(pedido);
  }

  @PostMapping("/orders/{id}/payment")
  public PaymentRedirectResponse payment(
      @PathVariable Long id,
      @RequestBody(required = false) PaymentRedirectRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Pedido pedido = pedidoService.buscarPorIdDoUsuario(id, userDetails.getUsername());
    String pagamentoSelecionado = request == null ? null : request.pagamento();
    Pagamento.MetodoPagamento metodo = pagamentoService.mapearMetodo(pagamentoSelecionado);
    String redirectUrl = pagamentoService.iniciarFluxoPagamento(pedido.getId(), metodo);
    return new PaymentRedirectResponse(redirectUrl);
  }

  private AccountOrderSummaryResponse toSummary(Pedido pedido) {
    return new AccountOrderSummaryResponse(
        pedido.getId(),
        pedido.getNumero(),
        pedido.getStatus().name(),
        pedido.getTotal(),
        pedido.getSubtotal(),
        pedido.getFrete(),
        pedido.getDataPedido().format(DATE_TIME_FORMATTER),
        pedido.getItens() == null
            ? 0
            : pedido.getItens().stream().mapToInt(item -> item.getQuantidade()).sum(),
        resolvePaymentStatus(pedido));
  }

  private AccountOrderDetailResponse toDetail(Pedido pedido) {
    Pagamento pagamento = findPagamento(pedido);
    return new AccountOrderDetailResponse(
        pedido.getId(),
        pedido.getNumero(),
        pedido.getStatus().name(),
        pedido.getTotal(),
        pedido.getSubtotal(),
        pedido.getFrete(),
        pedido.getDesconto() == null ? BigDecimal.ZERO : pedido.getDesconto(),
        pedido.getDataPedido().format(DATE_TIME_FORMATTER),
        pedido.getEnderecoEntrega(),
        pedido.getNomeDestinatario(),
        pedido.getTelefoneContato(),
        pedido.getCepEntrega(),
        pedido.getCidadeEntrega(),
        pedido.getEstadoEntrega(),
        pedido.getComplementoEntrega(),
        pedido.getObservacoes(),
        pagamento == null ? "SEM_PAGAMENTO" : pagamento.getStatus().name(),
        pagamento == null || pagamento.getMetodo() == null ? null : pagamento.getMetodo().name(),
        pagamento == null || pagamento.getPagoEm() == null
            ? null
            : pagamento.getPagoEm().format(DATE_TIME_FORMATTER),
        pedido.getItens().stream()
            .map(
                item ->
                    new AccountOrderItemResponse(
                        item.getProduto().getId(),
                        item.getProduto().getNome(),
                        item.getProduto().getImagemUrl(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()))))
            .toList());
  }

  private String resolvePaymentStatus(Pedido pedido) {
    Pagamento pagamento = findPagamento(pedido);
    return pagamento == null ? "SEM_PAGAMENTO" : pagamento.getStatus().name();
  }

  private Pagamento findPagamento(Pedido pedido) {
    try {
      return pagamentoService.buscarPorPedido(pedido.getId());
    } catch (RuntimeException ignored) {
      return null;
    }
  }
}
