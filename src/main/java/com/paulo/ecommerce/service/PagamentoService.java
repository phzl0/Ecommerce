package com.paulo.ecommerce.service;

import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.*;
import com.mercadopago.resources.preference.Preference;
import com.paulo.ecommerce.exception.RecursoNaoEncontradoException;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.model.Pagamento.*;
import com.paulo.ecommerce.repository.PagamentoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PedidoService pedidoService;
    private final EmailService emailService;
    private final PasseService passeService;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    public PagamentoService(PagamentoRepository pagamentoRepository,
                            PedidoService pedidoService,
                            EmailService emailService,
                            PasseService passeService) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoService = pedidoService;
        this.emailService = emailService;
        this.passeService = passeService;
    }

    @Transactional
    public String criarPreferencia(Long pedidoId) throws MPException, MPApiException {
        Pedido pedido = pedidoService.buscarPorId(pedidoId);

        Pagamento pagamento = pagamentoRepository
                .findByPedidoId(pedidoId)
                .orElse(new Pagamento());

        pagamento.setPedido(pedido);
        pagamento.setValor(pedido.getTotal());
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamentoRepository.save(pagamento);

        List<PreferenceItemRequest> itens = pedido.getItens().stream()
                .map(item -> PreferenceItemRequest.builder()
                        .title(item.getProduto().getNome())
                        .quantity(item.getQuantidade())
                        .unitPrice(item.getPrecoUnitario())
                        .currencyId("BRL")
                        .build())
                .toList();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(appUrl + "/pagamento/sucesso")
                .failure(appUrl + "/pagamento/falha")
                .pending(appUrl + "/pagamento/pendente")
                .build();

        PreferencePayerRequest payer = PreferencePayerRequest.builder()
                .email(pedido.getUsuario().getEmail())
                .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(itens)
                .payer(payer)
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference(String.valueOf(pedidoId))
                .notificationUrl(appUrl + "/pagamento/webhook")
                .statementDescriptor("MinhaLoja")
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        pagamento.setMpPreferenceId(preference.getId());
        pagamentoRepository.save(pagamento);

        return preference.getInitPoint();
    }

    public Pagamento buscarPorPreferenceId(String preferenceId) {
        return pagamentoRepository.findByMpPreferenceId(preferenceId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Pagamento", 0L));
    }

    @Transactional
    public void processarWebhook(String paymentId, String type) {
        if (!"payment".equals(type)) return;

        try {
            com.mercadopago.client.payment.PaymentClient client =
                    new com.mercadopago.client.payment.PaymentClient();

            com.mercadopago.resources.payment.Payment mp =
                    client.get(Long.parseLong(paymentId));

            String externalRef = mp.getExternalReference();
            if (externalRef == null) return;

            Long pedidoId = Long.parseLong(externalRef);

            Pagamento pagamento = pagamentoRepository
                    .findByPedidoId(pedidoId)
                    .orElseThrow(() ->
                            new RecursoNaoEncontradoException("Pagamento", pedidoId));

            pagamento.setMpPaymentId(paymentId);

            switch (mp.getStatus()) {
                case "approved" -> {
                    pagamento.setStatus(StatusPagamento.APROVADO);
                    pagamento.setPagoEm(LocalDateTime.now());

                    pedidoService.atualizarStatus(
                            pedidoId, Pedido.Status.CONFIRMADO);

                    passeService.registrarXpCompraAprovada(
                            pagamento.getPedido().getUsuario().getEmail(),
                            pedidoId,
                            pagamento.getPedido().getTotal()
                    );

                    emailService.enviarAtualizacaoStatus(
                            pedidoService.buscarPorId(pedidoId));
                }

                case "rejected" ->
                        pagamento.setStatus(StatusPagamento.RECUSADO);

                case "cancelled" ->
                        pagamento.setStatus(StatusPagamento.CANCELADO);

                case "refunded" -> {
                    pagamento.setStatus(StatusPagamento.REEMBOLSADO);

                    pedidoService.atualizarStatus(
                            pedidoId, Pedido.Status.CANCELADO);
                }
            }

            pagamentoRepository.save(pagamento);

        } catch (Exception e) {
            System.err.println("Erro ao processar webhook: " + e.getMessage());
        }
    }

    public Pagamento buscarPorPedido(Long pedidoId) {
        return pagamentoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Pagamento", pedidoId));
    }

    @Transactional
    public String iniciarFluxoPagamento(Long pedidoId, MetodoPagamento metodoPagamento) {
        try {
            Pagamento pagamento = pagamentoRepository.findByPedidoId(pedidoId)
                    .orElseGet(() -> {
                        Pagamento novo = new Pagamento();
                        novo.setPedido(pedidoService.buscarPorId(pedidoId));
                        return novo;
                    });

            pagamento.setMetodo(metodoPagamento);
            pagamento.setValor(pedidoService.buscarPorId(pedidoId).getTotal());

            pagamentoRepository.save(pagamento);

            return criarPreferencia(pedidoId);

        } catch (Exception e) {
            return "/pagamento/pedido/" + pedidoId;
        }
    }

    public MetodoPagamento mapearMetodo(String pagamento) {
        if (pagamento == null) {
            return MetodoPagamento.PIX;
        }

        return switch (pagamento.toLowerCase()) {
            case "cartao", "credito", "cartao_credito" ->
                    MetodoPagamento.CARTAO_CREDITO;
            case "boleto" ->
                    MetodoPagamento.BOLETO;
            default ->
                    MetodoPagamento.PIX;
        };
    }

    @Transactional
    public void atualizarStatusManual(String preferenceId, String status) {
        pagamentoRepository.findByMpPreferenceId(preferenceId)
                .ifPresent(pagamento -> {
                    switch (status) {
                        case "approved" -> {
                            pagamento.setStatus(StatusPagamento.APROVADO);
                            pagamento.setPagoEm(LocalDateTime.now());

                            pedidoService.atualizarStatus(
                                    pagamento.getPedido().getId(),
                                    Pedido.Status.CONFIRMADO);

                            passeService.registrarXpCompraAprovada(
                                    pagamento.getPedido().getUsuario().getEmail(),
                                    pagamento.getPedido().getId(),
                                    pagamento.getPedido().getTotal()
                            );
                        }

                        case "failure" ->
                                pagamento.setStatus(StatusPagamento.RECUSADO);

                        default ->
                                pagamento.setStatus(StatusPagamento.PENDENTE);
                    }

                    pagamentoRepository.save(pagamento);
                });
    }
}