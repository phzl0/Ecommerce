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

    @Value("${app.payments.enabled:false}")
    private boolean paymentsEnabled;
    @Value("${app.payments.live-mode:false}") private boolean liveMode;
    @Value("${app.payments.collector-id:}") private String collectorId;
    @Value("${mercadopago.access-token:}") private String accessToken;


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
        if (!paymentsEnabled) {
            throw new IllegalStateException("Pagamentos desativados neste ambiente.");
        }
        if (accessToken.isBlank() || collectorId.isBlank()) throw new IllegalStateException("Configure as credenciais e a conta recebedora do pagamento.");
        Pedido pedido = pedidoService.bloquearPedido(pedidoId);
        if (pedido.getStatus() != Pedido.Status.PENDENTE) throw new IllegalStateException("Este pedido não está disponível para pagamento.");

        Pagamento pagamento = pagamentoRepository
                .findByPedidoId(pedidoId)
                .orElse(new Pagamento());

        if (pagamento.getStatus() == StatusPagamento.APROVADO || pagamento.getStatus() == StatusPagamento.REEMBOLSADO) throw new IllegalStateException("Este pagamento já foi concluído.");
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
                .shipments(PreferenceShipmentsRequest.builder().mode("not_specified").cost(pedido.getFrete()).build())
                .expires(true)
                .expirationDateTo(java.time.OffsetDateTime.now().plusHours(24))
                .payer(payer)
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference(String.valueOf(pedidoId))
                .notificationUrl(appUrl + "/pagamento/webhook")
                .statementDescriptor("MinhaLoja")
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = criarNoProvedor(preferenceRequest);

        pagamento.setMpPreferenceId(preference.getId());
        pagamentoRepository.save(pagamento);

        return liveMode ? preference.getInitPoint() : preference.getSandboxInitPoint();
    }

    public Pagamento buscarPorPreferenceId(String preferenceId) {
        return pagamentoRepository.findByMpPreferenceId(preferenceId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Pagamento", 0L));
    }

    @Transactional
    public void processarWebhook(String paymentId, String type) {
        if (!paymentsEnabled || !"payment".equals(type)) throw new IllegalStateException("Pagamentos não disponíveis.");
        try {
            var mp = consultarPagamento(Long.parseLong(paymentId));
            if (mp.getExternalReference() == null) throw new IllegalArgumentException("Pagamento sem pedido.");
            Long pedidoId = Long.parseLong(mp.getExternalReference());
            Pedido pedido = pedidoService.bloquearPedido(pedidoId);
            Pagamento pagamento = pagamentoRepository.findByPedidoId(pedidoId).orElseThrow();
            if (!"BRL".equals(mp.getCurrencyId()) || mp.getTransactionAmount() == null || mp.getTransactionAmount().compareTo(pedido.getTotal()) != 0
                || !collectorId.equals(String.valueOf(mp.getCollectorId())) || !Boolean.valueOf(liveMode).equals(mp.isLiveMode())) {
                throw new IllegalStateException("O pagamento não corresponde ao valor, moeda, recebedor ou ambiente do pedido.");
            }
            if (pagamento.getMpPaymentId() != null && !pagamento.getMpPaymentId().equals(paymentId) && pagamento.getStatus() == StatusPagamento.APROVADO) {
                throw new IllegalStateException("Outro pagamento já foi aprovado para este pedido; requer conciliação.");
            }
            switch (mp.getStatus()) {
                case "approved" -> {
                    if (pagamento.getStatus() == StatusPagamento.APROVADO) return;
                    if (pedido.getStatus() != Pedido.Status.PENDENTE) throw new IllegalStateException("Pagamento recebido após encerramento do pedido; requer conciliação.");
                    pagamento.setMpPaymentId(paymentId);
                    pagamento.setStatus(StatusPagamento.APROVADO);
                    pagamento.setPagoEm(LocalDateTime.now());
                    pedidoService.atualizarStatus(pedidoId, Pedido.Status.CONFIRMADO);
                    if (pedido.getTemporadaPasseId() == null) passeService.registrarXpCompraAprovada(pedido.getUsuario().getEmail(), pedidoId, pedido.getSubtotal());
                }
                case "refunded", "charged_back" -> {
                    if (pagamento.getStatus() == StatusPagamento.REEMBOLSADO) return;
                    if (pagamento.getMpPaymentId() != null && !pagamento.getMpPaymentId().equals(paymentId)) throw new IllegalStateException("Estorno de tentativa diferente.");
                    pagamento.setStatus(StatusPagamento.REEMBOLSADO);
                    if (pedido.getStatus() != Pedido.Status.CANCELADO) pedidoService.atualizarStatus(pedidoId, Pedido.Status.CANCELADO);
                    passeService.estornarCompra(pedido.getUsuario().getEmail(), pedidoId);
                }
                case "rejected" -> { if (pagamento.getStatus() != StatusPagamento.APROVADO) pagamento.setStatus(StatusPagamento.RECUSADO); }
                case "cancelled" -> { if (pagamento.getStatus() != StatusPagamento.APROVADO) pagamento.setStatus(StatusPagamento.CANCELADO); }
                default -> { /* Do not regress an approved payment on an old pending event. */ }
            }
            pagamentoRepository.save(pagamento);
        } catch (com.mercadopago.exceptions.MPException | com.mercadopago.exceptions.MPApiException e) {
            throw new IllegalStateException("Não foi possível consultar o pagamento; a notificação precisa ser repetida.", e);
        }
    }

    public Pagamento buscarPorPedido(Long pedidoId) {
        return pagamentoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Pagamento", pedidoId));
    }

    @Transactional
    public String iniciarFluxoPagamento(Long pedidoId, MetodoPagamento metodoPagamento) {
        if (!paymentsEnabled) return "/pedidos/"+pedidoId+"?paymentsDisabled=true";
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

        } catch (MPException | MPApiException e) {
            throw new IllegalStateException("Não foi possível abrir o pagamento. Seu pedido está salvo para nova tentativa.", e);
        }
    }

    protected com.mercadopago.resources.payment.Payment consultarPagamento(Long id) throws MPException, MPApiException { return new com.mercadopago.client.payment.PaymentClient().get(id); }
    protected Preference criarNoProvedor(PreferenceRequest request) throws MPException, MPApiException { return new PreferenceClient().create(request); }

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

}
