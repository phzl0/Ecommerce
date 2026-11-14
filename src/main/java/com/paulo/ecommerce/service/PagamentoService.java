package com.paulo.ecommerce.service;

import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.*;
import com.mercadopago.resources.preference.Preference;
import com.paulo.ecommerce.exception.RecursoNaoEncontradoException;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.model.Pagamento.*;
import com.paulo.ecommerce.repository.PagamentoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoService {
  @org.springframework.beans.factory.annotation.Autowired private FidelidadeService fidelidade;

  private final PagamentoRepository pagamentoRepository;
  private final PedidoService pedidoService;
  private final EmailService emailService;
  private final PasseService passeService;
  private final EventoSemanalService eventos;
  private final org.springframework.core.env.Environment environment;

  @Value("${app.url:http://localhost:8080}")
  private String appUrl;

  @org.springframework.beans.factory.annotation.Autowired private ImpactoReembolsoService impactosReembolso;
  @Value("${app.payments.enabled:false}")
  private boolean paymentsEnabled;

  @Value("${app.payments.live-mode:false}")
  private boolean liveMode;

  @Value("${app.payments.collector-id:}")
  private String collectorId;

  @Value("${mercadopago.access-token:}")
  private String accessToken;

  public PagamentoService(
      PagamentoRepository pagamentoRepository,
      PedidoService pedidoService,
      EmailService emailService,
      PasseService passeService,
      EventoSemanalService eventos,
      org.springframework.core.env.Environment environment) {
    this.pagamentoRepository = pagamentoRepository;
    this.pedidoService = pedidoService;
    this.emailService = emailService;
    this.passeService = passeService;
    this.eventos = eventos;
    this.environment = environment;
  }

  public java.util.Map<String, Boolean> configuracaoPublica() {
    return java.util.Map.of(
        "enabled",
        paymentsEnabled
            && accessToken != null
            && !accessToken.isBlank()
            && collectorId != null
            && !collectorId.isBlank(),
        "liveMode",
        liveMode);
  }

  @Transactional
  public String criarPreferencia(Long pedidoId) throws MPException, MPApiException {
    if (!paymentsEnabled) {
      throw new IllegalStateException("Pagamentos desativados neste ambiente.");
    }
    if (accessToken.isBlank() || collectorId.isBlank())
      throw new IllegalStateException(
          "Configure as credenciais e a conta recebedora do pagamento.");
    Pedido pedido = pedidoService.bloquearPedido(pedidoId);
    if (pedido.getDataPedido().isBefore(LocalDateTime.now().minusHours(24)))
      throw new IllegalStateException("A reserva deste pedido expirou. Faça um novo pedido.");
    if (pedido.getStatus() != Pedido.Status.PENDENTE)
      throw new IllegalStateException("Este pedido não está disponível para pagamento.");

    Pagamento pagamento = pagamentoRepository.findByPedidoId(pedidoId).orElse(new Pagamento());

    if (pagamento.getStatus() == StatusPagamento.APROVADO
        || pagamento.getStatus() == StatusPagamento.REEMBOLSADO)
      throw new IllegalStateException("Este pagamento já foi concluído.");
    if (pagamento.getCheckoutUrl() != null && pagamento.getMpPreferenceId() != null)
      return pagamento.getCheckoutUrl();
    pagamento.setPedido(pedido);
    pagamento.setValor(pedido.getTotal());
    pagamento.setStatus(StatusPagamento.PENDENTE);
    pagamentoRepository.save(pagamento);

    List<PreferenceItemRequest> itens =
        pedido.getItens().stream()
            .map(
                item ->
                    PreferenceItemRequest.builder()
                        .title(item.getProduto().getNome())
                        .quantity(item.getQuantidade())
                        .unitPrice(item.getPrecoUnitario())
                        .currencyId("BRL")
                        .build())
            .toList();

    if (pedido.getDesconto().signum() > 0)
      itens =
          List.of(
              PreferenceItemRequest.builder()
                  .title("Produtos do pedido " + pedido.getNumero())
                  .quantity(1)
                  .unitPrice(pedido.getSubtotal().subtract(pedido.getDesconto()))
                  .currencyId("BRL")
                  .build());
    PreferenceBackUrlsRequest backUrls =
        PreferenceBackUrlsRequest.builder()
            .success(appUrl + "/pagamento/sucesso")
            .failure(appUrl + "/pagamento/falha")
            .pending(appUrl + "/pagamento/pendente")
            .build();

    PreferencePayerRequest payer =
        PreferencePayerRequest.builder().email(pedido.getUsuario().getEmail()).build();

    PreferenceRequest preferenceRequest =
        PreferenceRequest.builder()
            .items(itens)
            .shipments(
                PreferenceShipmentsRequest.builder()
                    .mode("not_specified")
                    .cost(pedido.getFrete())
                    .build())
            .expires(true)
            .expirationDateTo(
                pedido
                    .getDataPedido()
                    .atZone(java.time.ZoneId.systemDefault())
                    .plusHours(24)
                    .toOffsetDateTime())
            .payer(payer)
            .backUrls(backUrls)
            .autoReturn("approved")
            .externalReference(String.valueOf(pedidoId))
            .notificationUrl(appUrl + "/pagamento/webhook")
            .statementDescriptor("MinhaLoja")
            .build();

    PreferenceClient client = new PreferenceClient();
    Preference preference = criarNoProvedor(preferenceRequest);

    pagamento.setCheckoutUrl(
        liveMode ? preference.getInitPoint() : preference.getSandboxInitPoint());
    pagamento.setMpPreferenceId(preference.getId());
    pagamentoRepository.save(pagamento);

    return liveMode ? preference.getInitPoint() : preference.getSandboxInitPoint();
  }

  public Pagamento buscarPorPreferenceId(String preferenceId) {
    return pagamentoRepository
        .findByMpPreferenceId(preferenceId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento", 0L));
  }

  @Transactional
  public void processarWebhook(String paymentId, String type) {
    if (!paymentsEnabled || !"payment".equals(type))
      throw new IllegalStateException("Pagamentos não disponíveis.");
    try {
      var mp = consultarPagamento(Long.parseLong(paymentId));
      if (mp.getExternalReference() == null)
        throw new IllegalArgumentException("Pagamento sem pedido.");
      Long pedidoId = Long.parseLong(mp.getExternalReference());
      Pedido pedido = pedidoService.bloquearPedidoParaConciliacao(pedidoId);
      Pagamento pagamento = pagamentoRepository.findByPedidoId(pedidoId).orElseThrow();
      if (!"BRL".equals(mp.getCurrencyId())
          || mp.getTransactionAmount() == null
          || mp.getTransactionAmount().compareTo(pedido.getTotal()) != 0
          || !collectorId.equals(String.valueOf(mp.getCollectorId()))
          || !Boolean.valueOf(liveMode).equals(mp.isLiveMode())) {
        throw new IllegalStateException(
            "O pagamento não corresponde ao valor, moeda, recebedor ou ambiente do pedido.");
      }
      if (pagamento.getMpPaymentId() != null
          && !pagamento.getMpPaymentId().equals(paymentId)
          && pagamento.getStatus() == StatusPagamento.APROVADO) {
        throw new IllegalStateException(
            "Outro pagamento já foi aprovado para este pedido; requer conciliação.");
      }
      switch (mp.getStatus()) {
        case "approved" -> {
          if (pagamento.getStatus() == StatusPagamento.APROVADO) return;
          if (pedido.getDataPedido().isBefore(LocalDateTime.now().minusHours(24)))
            throw new IllegalStateException("A reserva deste pedido expirou. Faça um novo pedido.");
          if (pedido.getStatus() != Pedido.Status.PENDENTE)
            throw new IllegalStateException(
                "Pagamento recebido após encerramento do pedido; requer conciliação.");
          pagamento.setMpPaymentId(paymentId);
          confirmar(pedido, pagamento);
        }
        case "refunded", "charged_back" -> {
          if (pagamento.getStatus() == StatusPagamento.REEMBOLSADO) return;
          if (pagamento.getMpPaymentId() != null && !pagamento.getMpPaymentId().equals(paymentId))
            throw new IllegalStateException("Estorno de tentativa diferente.");
          var restante=pedido.getTotal().subtract(pedido.getValorReembolsado());
          var aviso=impactosReembolso.prever(pedido,restante);
          impactosReembolso.aplicar(pedido,restante,aviso.token());
          pedido.setValorReembolsado(pedido.getTotal());
          pagamento.setStatus(StatusPagamento.REEMBOLSADO);
          if (pedido.getStatus() != Pedido.Status.CANCELADO)
            pedidoService.atualizarStatus(pedidoId, Pedido.Status.CANCELADO);

        }
        case "rejected" -> {
          if (pagamento.getStatus() != StatusPagamento.APROVADO
              && pagamento.getStatus() != StatusPagamento.REEMBOLSADO)
            pagamento.setStatus(StatusPagamento.RECUSADO);
        }
        case "cancelled" -> {
          if (pagamento.getStatus() != StatusPagamento.APROVADO
              && pagamento.getStatus() != StatusPagamento.REEMBOLSADO)
            pagamento.setStatus(StatusPagamento.CANCELADO);
        }
        default -> {
          /* Do not regress an approved payment on an old pending event. */
        }
      }
      pagamentoRepository.save(pagamento);
    } catch (com.mercadopago.exceptions.MPException | com.mercadopago.exceptions.MPApiException e) {
      throw new IllegalStateException(
          "Não foi possível consultar o pagamento; a notificação precisa ser repetida.", e);
    }
  }

  public boolean simulacaoDisponivel() {
    return !paymentsEnabled && environment.matchesProfiles("local & !prod")
        && environment.getProperty("app.local-demo-enabled", Boolean.class, false);
  }

  @Transactional
  public void simularAprovacao(Long pedidoId) {
    if (!simulacaoDisponivel()) throw new IllegalStateException("Simulação indisponível.");
    Pedido pedido = pedidoService.bloquearPedidoParaConciliacao(pedidoId);
    if (pedido.getStatus() != Pedido.Status.PENDENTE
        || pedido.getDataPedido().isBefore(LocalDateTime.now().minusHours(24)))
      throw new IllegalStateException("O pedido não tem uma reserva válida para simular.");
    if (pagamentoRepository.findByPedidoId(pedidoId).isPresent())
      throw new IllegalStateException("Este pedido já possui uma tentativa de pagamento.");
    Pagamento pagamento = new Pagamento();
    pagamento.setPedido(pedido);
    pagamento.setMetodo(MetodoPagamento.PIX);
    pagamento.setValor(pedido.getTotal());
    pedido.setPagamento(pagamento);
    confirmar(pedido, pagamento);
  }

  private void confirmar(Pedido pedido, Pagamento pagamento) {
    if (pedido.getTemporadaPasseId() != null) passeService.validarTemporadaCompra(pedido.getTemporadaPasseId());
    pagamento.setStatus(StatusPagamento.APROVADO);
    pagamento.setPagoEm(LocalDateTime.now());
    pagamentoRepository.save(pagamento);
    pedidoService.atualizarStatus(pedido.getId(), Pedido.Status.CONFIRMADO);
    fidelidade.compra(pedido);
    if (pedido.getTemporadaPasseId() == null) {
      var valor = pedido.getSubtotal().subtract(pedido.getDesconto());
      int extra=pedido.getItens()==null?0:Math.toIntExact(pedido.getItens().stream().mapToLong(i->(long)i.getXpExtraSnapshot()*i.getQuantidade()).sum());
      if(extra>0)passeService.registrarXpCompraAprovada(pedido.getUsuario().getEmail(),pedido.getId(),valor,extra);
      else passeService.registrarXpCompraAprovada(pedido.getUsuario().getEmail(), pedido.getId(), valor);
      eventos.notificarMeta(pedido.getUsuario().getId(), pagamento.getPagoEm(), valor);
    }
  }

  public Pagamento buscarPorPedido(Long pedidoId) {
    return pagamentoRepository
        .findByPedidoId(pedidoId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento", pedidoId));
  }

  @Transactional
  public String iniciarFluxoPagamento(Long pedidoId, MetodoPagamento metodoPagamento) {
    if (pedidoService.buscarPorId(pedidoId).isRecompensa() || pedidoService.buscarPorId(pedidoId).getPedidoOrigemId()!=null)
      throw new IllegalStateException("Recompensas não exigem pagamento.");
    if (!paymentsEnabled) return "/pedidos/" + pedidoId + "?paymentsDisabled=true";
    try {
      Pagamento pagamento =
          pagamentoRepository
              .findByPedidoId(pedidoId)
              .orElseGet(
                  () -> {
                    Pagamento novo = new Pagamento();
                    novo.setPedido(pedidoService.buscarPorId(pedidoId));
                    return novo;
                  });

      pagamento.setMetodo(metodoPagamento);
      pagamento.setValor(pedidoService.buscarPorId(pedidoId).getTotal());

      pagamentoRepository.save(pagamento);

      return criarPreferencia(pedidoId);

    } catch (MPException | MPApiException e) {
      throw new IllegalStateException(
          "Não foi possível abrir o pagamento. Seu pedido está salvo para nova tentativa.", e);
    }
  }

  protected com.mercadopago.resources.payment.Payment consultarPagamento(Long id)
      throws MPException, MPApiException {
    return new com.mercadopago.client.payment.PaymentClient().get(id);
  }

  protected Preference criarNoProvedor(PreferenceRequest request)
      throws MPException, MPApiException {
    return new PreferenceClient().create(request);
  }

  public MetodoPagamento mapearMetodo(String pagamento) {
    if (pagamento == null) {
      return MetodoPagamento.PIX;
    }

    return switch (pagamento.toLowerCase()) {
      case "cartao", "credito", "cartao_credito" -> MetodoPagamento.CARTAO_CREDITO;
      case "boleto" -> MetodoPagamento.BOLETO;
      default -> MetodoPagamento.PIX;
    };
  }
}
