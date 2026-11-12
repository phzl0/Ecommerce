package com.paulo.ecommerce.service;

import com.paulo.ecommerce.dto.CheckoutForm;
import com.paulo.ecommerce.dto.api.CheckoutItemRequest;
import com.paulo.ecommerce.exception.CarrinhoVazioException;
import com.paulo.ecommerce.exception.EstoqueInsuficienteException;
import com.paulo.ecommerce.exception.RecursoNaoEncontradoException;
import com.paulo.ecommerce.model.ItemPedido;
import com.paulo.ecommerce.model.Pedido;
import com.paulo.ecommerce.model.PedidoMovimentacao;
import com.paulo.ecommerce.model.Pagamento;
import com.paulo.ecommerce.model.Produto;
import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.repository.PedidoRepository;
import com.paulo.ecommerce.repository.PedidoMovimentacaoRepository;
import com.paulo.ecommerce.repository.ProdutoRepository;
import com.paulo.ecommerce.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.UUID;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

  @org.springframework.beans.factory.annotation.Autowired private PasseService passes;
  @org.springframework.beans.factory.annotation.Autowired private NotificacaoService notificacoes;
  @org.springframework.beans.factory.annotation.Autowired private EstoqueService estoque;
  @org.springframework.beans.factory.annotation.Autowired private DigitalService digitais;
  @org.springframework.beans.factory.annotation.Autowired private com.paulo.ecommerce.repository.BauItemRepository bauItens;
  private final PedidoRepository pedidoRepository;
  private final PedidoMovimentacaoRepository movimentacoes;
  private final ProdutoRepository produtoRepository;
  private final UsuarioRepository usuarioRepository;
  private final CarrinhoService carrinhoService;
  private final EmailService emailService;

  public PedidoService(
      PedidoRepository pedidoRepository,
      PedidoMovimentacaoRepository movimentacoes,
      ProdutoRepository produtoRepository,
      UsuarioRepository usuarioRepository,
      CarrinhoService carrinhoService,
      EmailService emailService) {
    this.pedidoRepository = pedidoRepository;
    this.movimentacoes = movimentacoes;
    this.produtoRepository = produtoRepository;
    this.usuarioRepository = usuarioRepository;
    this.carrinhoService = carrinhoService;
    this.emailService = emailService;
  }

  @org.springframework.cache.annotation.CacheEvict(
      value = {"produto", "produtos", "metricas"},
      allEntries = true)
  @Transactional
  public Pedido finalizarPedido(
      String emailUsuario, CheckoutForm checkoutForm, HttpSession session) {
    Usuario usuario =
        usuarioRepository
            .findLockedByEmail(emailUsuario)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", 0L));

    List<CheckoutItemRequest> itensCarrinho =
        carrinhoService.getCarrinho(session).stream()
            .map(item -> new CheckoutItemRequest(item.getProdutoId(), item.getQuantidade()))
            .toList();

    Pedido pedido = criarPedido(usuario, checkoutForm, itensCarrinho);
    carrinhoService.limpar(session);
    return pedido;
  }

  @org.springframework.cache.annotation.CacheEvict(
      value = {"produto", "produtos", "metricas"},
      allEntries = true)
  @Transactional
  public Pedido finalizarPedidoApi(
      String emailUsuario,
      CheckoutForm checkoutForm,
      List<CheckoutItemRequest> itensRequest,
      String idempotencyKey) {
    Usuario usuario =
        usuarioRepository
            .findLockedByEmail(emailUsuario)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", 0L));

    String key = usuario.getId() + "-" + idempotencyKey;
    var existente = pedidoRepository.findByUsuarioIdAndCheckoutKey(usuario.getId(), key);
    if (existente.isPresent()) return existente.get();
    Pedido pedido = criarPedido(usuario, checkoutForm, itensRequest);
    pedido.setCheckoutKey(key);
    return pedidoRepository.save(pedido);
  }

  public record Cotacao(BigDecimal subtotal, BigDecimal frete, BigDecimal desconto, BigDecimal total,
      Map<Long, BigDecimal> precos, String cupomErro) {}

  @Transactional(readOnly = true)
  public Cotacao cotar(String email, String cupom, List<CheckoutItemRequest> itens) {
    Map<Long, Integer> quantidades = new HashMap<>();
    for (var item : itens) quantidades.merge(item.produtoId(), item.quantidade(), Integer::sum);
    Map<Long, BigDecimal> precos = new HashMap<>();
    BigDecimal subtotal = BigDecimal.ZERO;
    for (var produto : produtoRepository.findAllById(quantidades.keySet())) {
      int quantidade = quantidades.get(produto.getId());
      validarDisponibilidade(produto, quantidade);
      precos.put(produto.getId(), produto.getPreco());
      subtotal = subtotal.add(produto.getPreco().multiply(BigDecimal.valueOf(quantidade)));
    }
    if (precos.size() != quantidades.size()) {
      throw new IllegalArgumentException("Um produto da sacola não está mais disponível.");
    }
    validarSubtotal(subtotal);
    boolean fisico = produtoRepository.findAllById(quantidades.keySet()).stream()
        .anyMatch(p -> p.getTipo() == Produto.Tipo.FISICO);
    BigDecimal frete = fisico ? calcularFrete(subtotal) : BigDecimal.ZERO;
    BigDecimal desconto = BigDecimal.ZERO;
    String cupomErro = null;
    if (cupom != null && !cupom.isBlank()) {
      if (email == null) {
        cupomErro = "Entre na sua conta para aplicar um cupom.";
      } else {
        var usuario = usuarioRepository.findByEmail(email).orElseThrow();
        try {
          desconto = passes.consultarDescontoCupom(usuario, cupom, subtotal);
        } catch (IllegalArgumentException | IllegalStateException erro) {
          cupomErro = erro.getMessage();
        }
      }
    }
    return new Cotacao(subtotal, frete, desconto, subtotal.add(frete).subtract(desconto), precos, cupomErro);
  }

  private Pedido criarPedido(
      Usuario usuario, CheckoutForm checkoutForm, List<CheckoutItemRequest> itensCarrinho) {

    if (itensCarrinho.isEmpty()) {
      throw new CarrinhoVazioException();
    }

    Pedido pedido = new Pedido();
    pedido.setUsuario(usuario);
    pedido.setNumero("LC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 26));
    pedido.setNomeDestinatario(checkoutForm.getNomeDestinatario());
    pedido.setTelefoneContato(checkoutForm.getTelefoneContato());
    pedido.setCepEntrega(checkoutForm.getCep());
    pedido.setCidadeEntrega(checkoutForm.getCidade());
    pedido.setEstadoEntrega(checkoutForm.getUf().toUpperCase(Locale.ROOT));
    pedido.setComplementoEntrega(checkoutForm.getComplemento());
    pedido.setObservacoes(checkoutForm.getObservacoes());
    pedido.setEnderecoEntrega(checkoutForm.getEnderecoCompleto());
    List<ItemPedido> itens = new ArrayList<>();
    for (CheckoutItemRequest ci :
        itensCarrinho.stream()
            .sorted(Comparator.comparing(CheckoutItemRequest::produtoId))
            .toList()) {
      Produto produto =
          produtoRepository
              .findLockedById(ci.produtoId())
              .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", ci.produtoId()));

      validarDisponibilidade(produto, ci.quantidade());



      ItemPedido item = new ItemPedido();
      item.setPedido(pedido);
      item.setProduto(produto);
      item.setTipo(produto.getTipo());
      item.setQuantidade(ci.quantidade());
      item.setPrecoUnitario(produto.getPreco());
      itens.add(item);
    }

    java.math.BigDecimal subtotal =
        itens.stream()
            .map(
                i -> i.getPrecoUnitario().multiply(java.math.BigDecimal.valueOf(i.getQuantidade())))
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    validarSubtotal(subtotal);
    pedido.setSubtotal(subtotal);
    pedido.setFrete(itens.stream().anyMatch(i -> i.getProduto().getTipo() == Produto.Tipo.FISICO)
        ? calcularFrete(subtotal) : BigDecimal.ZERO);
    pedido.setTotal(subtotal.add(pedido.getFrete()));
    pedido.setItens(itens);
    Pedido salvo = pedidoRepository.saveAndFlush(pedido);
    Map<Long,Integer> totais = new java.util.TreeMap<>();
    for(var item : itens) totais.merge(item.getProduto().getId(),item.getQuantidade(),Math::addExact);
    for(var entry : totais.entrySet()) estoque.reservar(produtoRepository.findLockedById(entry.getKey()).orElseThrow(),entry.getValue(),"PEDIDO:"+salvo.getId());
    digitais.reservar(salvo);
    if (checkoutForm.getCupom() != null && !checkoutForm.getCupom().isBlank()) {
      salvo.setDesconto(passes.usarCupom(usuario, checkoutForm.getCupom(), salvo));
      salvo.setTotal(salvo.getSubtotal().add(salvo.getFrete()).subtract(salvo.getDesconto()));
      salvo = pedidoRepository.save(salvo);
    }
    String mensagem = "Acompanhe o pedido " + salvo.getNumero() + " na sua conta.";
    if (salvo.getDesconto().signum() > 0) mensagem += " Cupom aplicado.";
    notificacoes.registrar(usuario.getId(), "Pedido recebido", mensagem, "/pedidos/" + salvo.getId());
    emailService.enviarConfirmacaoPedido(salvo);
    return salvo;
  }

  public List<Pedido> listarPorUsuario(String email) {
    Usuario usuario =
        usuarioRepository
            .findByEmail(email)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", 0L));
    return pedidoRepository.findByUsuarioIdOrderByDataPedidoDesc(usuario.getId());
  }

  public List<Pedido> listarTodos() {
    return pedidoRepository.findAll(Sort.by("dataPedido").descending());
  }

  @Transactional
  public Pedido bloquearPedido(Long id) {
    return pedidoRepository
        .findLockedById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
  }

  @Transactional
  public Pedido bloquearPedidoParaConciliacao(Long id) {
    // Checkout and purchase XP lock the customer before any shared stock is changed.
    String email =
        pedidoRepository.findOwnerEmail(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    usuarioRepository.findLockedByEmail(email)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", 0L));
    return bloquearPedido(id);
  }

  public Pedido buscarPorId(Long id) {
    return pedidoRepository
        .findByIdComDetalhes(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
  }

  public Pedido buscarPorIdDoUsuario(Long id, String emailUsuario) {
    Pedido pedido = buscarPorId(id);
    if (!pedido.getUsuario().getEmail().equalsIgnoreCase(emailUsuario)) {
      throw new org.springframework.security.access.AccessDeniedException(
          "Pedido nao pertence ao usuario autenticado.");
    }
    return pedido;
  }

  public List<PedidoMovimentacao> historico(Long id) {
    return movimentacoes.findByPedidoIdOrderByIdDesc(id);
  }

  public List<Pedido.Status> acoesOperacionais(Pedido pedido) {
    List<Pedido.Status> acoes = new ArrayList<>();
    boolean fisico = pedido.getTemporadaPasseId() == null && pedido.getItens()!=null && pedido.getItens().stream().anyMatch(i -> i.getTipo() == Produto.Tipo.FISICO);
    boolean pagamentoAprovado = pedido.isRecompensa() || (pedido.getPagamento() != null
        && pedido.getPagamento().getStatus() == Pagamento.StatusPagamento.APROVADO);
    if (fisico && pagamentoAprovado && pedido.getStatus() == Pedido.Status.CONFIRMADO) {
      acoes.add(Pedido.Status.ENVIADO);
    }
    if (fisico && pagamentoAprovado && pedido.getStatus() == Pedido.Status.ENVIADO) {
      acoes.add(Pedido.Status.ENTREGUE);
    }
    if ((!pagamentoAprovado && pedido.getStatus() == Pedido.Status.PENDENTE)
        || (pedido.isRecompensa() && pedido.getStatus() == Pedido.Status.CONFIRMADO)) {
      acoes.add(Pedido.Status.CANCELADO);
    }
    return acoes;
  }

  @org.springframework.cache.annotation.CacheEvict(
      value = {"produto", "produtos", "metricas"}, allEntries = true)
  @Transactional
  public void atualizarStatusOperacional(Long id, Pedido.Status status) {
    Pedido pedido = bloquearPedido(id);
    if (!acoesOperacionais(pedido).contains(status)) {
      throw new IllegalStateException("Esta ação não está disponível para o estado atual do pedido.");
    }
    aplicarStatus(pedido, status);
  }

  @org.springframework.cache.annotation.CacheEvict(
      value = {"produto", "produtos", "metricas"},
      allEntries = true)
  @Transactional
  public void atualizarStatus(Long id, Pedido.Status status) {
    Pedido pedido =
        pedidoRepository
            .findLockedById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    aplicarStatus(pedido, status);
  }

  private void aplicarStatus(Pedido pedido, Pedido.Status status) {
    if (pedido.getStatus() == status) return;
    if (pedido.getStatus() == Pedido.Status.CANCELADO
        || status == Pedido.Status.PENDENTE
        || (status == Pedido.Status.CONFIRMADO && pedido.getStatus() != Pedido.Status.PENDENTE))
      throw new IllegalStateException("Transição de pedido inválida.");
    if (status == Pedido.Status.ENVIADO && pedido.getStatus() != Pedido.Status.CONFIRMADO)
      throw new IllegalStateException("Confirme o pagamento antes do envio.");
    if (status == Pedido.Status.ENTREGUE && pedido.getStatus() != Pedido.Status.ENVIADO)
      throw new IllegalStateException("O pedido ainda não foi enviado.");
    if (status == Pedido.Status.CONFIRMADO
        && (pedido.getPagamento() == null
            || pedido.getPagamento().getStatus()
                != com.paulo.ecommerce.model.Pagamento.StatusPagamento.APROVADO))
      throw new IllegalStateException("Pagamento ainda não aprovado.");
    if (status == Pedido.Status.CANCELADO) {
      if (pedido.getPagamento() != null
          && pedido.getPagamento().getStatus()
              == com.paulo.ecommerce.model.Pagamento.StatusPagamento.APROVADO)
        throw new IllegalStateException(
            "O pagamento aprovado precisa ser estornado no provedor antes do cancelamento.");
      if (pedido.getStatus() == Pedido.Status.PENDENTE
          || pedido.getStatus() == Pedido.Status.CONFIRMADO) devolverEstoque(pedido);
    }
    if(status == Pedido.Status.CONFIRMADO && pedido.getTemporadaPasseId()==null) digitais.liberar(pedido);
    if(status == Pedido.Status.CANCELADO) digitais.revogar(pedido);
    if(status == Pedido.Status.ENVIADO) {
      if(pedido.isRecompensa()) bauItens.findByPedidoId(pedido.getId()).ifPresent(b -> estoque.consumir("BAU:"+b.id,false));
      else estoque.consumir("PEDIDO:"+pedido.getId(),false);
    }
    pedido.setStatus(status);
    pedidoRepository.save(pedido);
    String titulo = switch (status) {
      case CONFIRMADO -> "Pagamento confirmado";
      case ENVIADO -> "Pedido enviado";
      case ENTREGUE -> "Pedido entregue";
      case CANCELADO -> "Pedido cancelado";
      case PENDENTE -> throw new IllegalStateException("Transição de pedido inválida.");
    };
    registrarMovimentacao(pedido, titulo);
    notificarStatus(pedido, titulo);
    emailService.enviarAtualizacaoStatus(pedido);
  }

  @org.springframework.cache.annotation.CacheEvict(
      value = {"produto", "produtos", "metricas"},
      allEntries = true)
  @Transactional
  public void cancelarPedido(Long id, String emailUsuario) {
    Pedido pedido =
        pedidoRepository
            .findLockedById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));

    if (!pedido.getUsuario().getEmail().equals(emailUsuario)) {
      throw new org.springframework.security.access.AccessDeniedException(
          "Pedido nao pertence ao usuario.");
    }

    if (pedido.getStatus() != Pedido.Status.PENDENTE) {
      throw new RuntimeException("Apenas pedidos pendentes podem ser cancelados.");
    }

    devolverEstoque(pedido);

    pedido.setStatus(Pedido.Status.CANCELADO);
    pedidoRepository.save(pedido);
    registrarMovimentacao(pedido, "Pedido cancelado pelo cliente");
    notificarStatus(pedido, "Pedido cancelado");
  }

  @org.springframework.cache.annotation.CacheEvict(
      value = {"produto", "produtos", "metricas"},
      allEntries = true)
  @Transactional
  public void expirarReserva(Long id) {
    Pedido pedido = bloquearPedido(id);
    if (pedido.getStatus() != Pedido.Status.PENDENTE
        || !pedido.getDataPedido().isBefore(java.time.LocalDateTime.now().minusHours(24))) return;
    if (pedido.getPagamento() != null
        && pedido.getPagamento().getStatus()
            == com.paulo.ecommerce.model.Pagamento.StatusPagamento.APROVADO) return;
    devolverEstoque(pedido);
    pedido.setStatus(Pedido.Status.CANCELADO);
    pedidoRepository.save(pedido);
    registrarMovimentacao(pedido, "Reserva do pedido expirada");
    notificarStatus(pedido, "Reserva do pedido expirada");
  }

  private void registrarMovimentacao(Pedido pedido, String descricao) {
    movimentacoes.save(new PedidoMovimentacao(pedido.getId(), pedido.getStatus(), descricao));
  }

  private void notificarStatus(Pedido pedido, String titulo) {
    notificacoes.registrar(pedido.getUsuario().getId(), titulo,
        "Pedido " + pedido.getNumero(), "/pedidos/" + pedido.getId());
  }

  private void devolverEstoque(Pedido pedido) {
    if (pedido.getTemporadaPasseId() != null || pedido.isRecompensa()) return;
    estoque.liberar("PEDIDO:"+pedido.getId());
    digitais.revogar(pedido);
  }

  private void validarDisponibilidade(Produto produto, int quantidade) {
    if (!produto.isAtivo() || quantidade <= 0 || produto.getEstoque() < quantidade) {
      throw new EstoqueInsuficienteException(produto.getNome());
    }
  }

  private void validarSubtotal(BigDecimal subtotal) {
    if (subtotal.compareTo(new BigDecimal("9999999.99")) > 0) {
      throw new IllegalArgumentException("O valor deste pedido excede o limite suportado.");
    }
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
