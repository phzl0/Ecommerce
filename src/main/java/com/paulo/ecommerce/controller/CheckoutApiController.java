package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.dto.CheckoutForm;
import com.paulo.ecommerce.dto.api.CheckoutRequest;
import com.paulo.ecommerce.dto.api.CheckoutResponse;
import com.paulo.ecommerce.model.Pagamento;
import com.paulo.ecommerce.service.PagamentoService;
import com.paulo.ecommerce.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutApiController {

  private final PedidoService pedidoService;
  private final PagamentoService pagamentoService;

  public CheckoutApiController(PedidoService pedidoService, PagamentoService pagamentoService) {
    this.pedidoService = pedidoService;
    this.pagamentoService = pagamentoService;
  }

  @org.springframework.web.bind.annotation.GetMapping("/config")
  public java.util.Map<String, Boolean> config() {
    return pagamentoService.configuracaoPublica();
  }

  @PostMapping("/orders")
  public CheckoutResponse criarPedido(
      @Valid @RequestBody CheckoutRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    CheckoutForm form = new CheckoutForm();
    form.setCupom(request.getCupom());
    form.setNomeDestinatario(request.getNome());
    form.setTelefoneContato(request.getTelefone());
    form.setCep(request.getCep());
    form.setLogradouro(request.getEndereco());
    form.setNumero(request.getNumero());
    form.setComplemento(request.getComplemento());
    form.setBairro(request.getBairro());
    form.setCidade(request.getCidade());
    form.setUf(request.getEstado());
    form.setObservacoes("Pagamento selecionado: " + request.getPagamento());

    var pedido =
        pedidoService.finalizarPedidoApi(
            userDetails.getUsername(), form, request.getItens(), request.getIdempotencyKey());

    Pagamento.MetodoPagamento metodo = pagamentoService.mapearMetodo(request.getPagamento());
    String redirectUrl;
    try {
      redirectUrl = pagamentoService.iniciarFluxoPagamento(pedido.getId(), metodo);
    } catch (RuntimeException error) {
      redirectUrl = "/pedidos/" + pedido.getId() + "?paymentError=true";
    }
    return new CheckoutResponse(pedido.getId(), pedido.getNumero(), redirectUrl);
  }
}
