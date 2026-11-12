package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.service.*;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
  @org.springframework.beans.factory.annotation.Autowired
  private com.paulo.ecommerce.service.ProductImageStorage imageStorage;

  @org.springframework.beans.factory.annotation.Autowired
  private com.paulo.ecommerce.service.PagamentoService payments;

  private final AdminService adminService;
  private final ProdutoService produtoService;
  private final CategoriaService categoriaService;
  private final PedidoService pedidoService;

  public AdminController(
      AdminService adminService,
      ProdutoService produtoService,
      CategoriaService categoriaService,
      PedidoService pedidoService) {
    this.adminService = adminService;
    this.produtoService = produtoService;
    this.categoriaService = categoriaService;
    this.pedidoService = pedidoService;
  }

  @InitBinder("produto")
  void restringirCamposProduto(WebDataBinder binder) {
    binder.setAllowedFields("id", "nome", "sku", "marca", "descricaoCurta", "descricao", "preco", "estoque", "destaque", "tipo");
  }

  @GetMapping("/integracoes")
  public String integracoes() {
    return "admin/integracoes";
  }

  // ── Dashboard ──────────────────────────────────────────
  @GetMapping
  public String dashboard(Model model) {
    model.addAttribute("totalPedidos", adminService.totalPedidos());
    model.addAttribute("totalProdutos", adminService.totalProdutos());
    model.addAttribute("totalClientes", adminService.totalClientes());
    model.addAttribute("receitaTotal", adminService.receitaTotal());
    model.addAttribute("pedidosRecentes", adminService.pedidosRecentes());
    model.addAttribute("produtosSemEstoque", adminService.produtosSemEstoque());
    model.addAttribute("estoqueBaixo", adminService.produtosComEstoqueBaixo());
    model.addAttribute("eventosAtivos", adminService.eventosAtivos());
    model.addAttribute("pedidosPendentes", adminService.pedidosPendentes());
    model.addAttribute("pedidosParaEnvio", adminService.pedidosParaEnvio());
    model.addAttribute("ambienteDeTeste", payments.simulacaoDisponivel());
    return "admin/dashboard";
  }

  // ── Produtos ───────────────────────────────────────────
  @GetMapping("/produtos")
  public String produtos(Model model, @RequestParam(defaultValue = "") String busca,
      @RequestParam(defaultValue = "false") boolean semEstoque,
      @RequestParam(defaultValue = "false") boolean estoqueBaixo,
      @RequestParam(defaultValue = "ativos") String visibilidade,
      @RequestParam(defaultValue = "0") int pagina) {
    Boolean ativo = switch (visibilidade) {
      case "todos" -> null;
      case "inativos" -> false;
      default -> true;
    };
    var resultados = adminService.produtos(busca.strip(), ativo, semEstoque, estoqueBaixo && !semEstoque, pagina);
    model.addAttribute("produtos", resultados.getContent());
    model.addAttribute("pagina", resultados);
    model.addAttribute("busca", busca.strip());
    model.addAttribute("semEstoque", semEstoque);
    model.addAttribute("estoqueBaixo", estoqueBaixo && !semEstoque);
    model.addAttribute("limiteEstoque", AdminService.LIMITE_ESTOQUE_BAIXO);
    model.addAttribute("visibilidade", ativo == null ? "todos" : ativo ? "ativos" : "inativos");
    return "admin/produtos/lista";
  }

  @GetMapping("/produtos/novo")
  public String novoProdutoForm(Model model) {
    model.addAttribute("produto", new Produto());
    model.addAttribute("categorias", categoriaService.listarTodas());
    return "admin/produtos/form";
  }

  @GetMapping("/produtos/{id}/editar")
  public String editarProdutoForm(@PathVariable Long id, Model model) {
    Produto produto = produtoService.buscarPorId(id);
    model.addAttribute("produto", produto);
    model.addAttribute("estoqueOriginal", produto.getEstoque());
    model.addAttribute("categorias", categoriaService.listarTodas());
    return "admin/produtos/form";
  }

  @PostMapping("/produtos/salvar")
  public String salvarProduto(
      @Valid @ModelAttribute("produto") Produto produto,
      BindingResult result,
      @RequestParam(required = false) MultipartFile imagem,
      @RequestParam(required = false) Long categoriaId,
      @RequestParam(required = false) Integer estoqueOriginal,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (!result.hasErrors()) {
      try {
        if (categoriaId != null) produto.setCategoria(categoriaService.buscarPorId(categoriaId));
        if (imagem != null && !imagem.isEmpty()) produto.setImagemUrl(imageStorage.save(imagem));
        produtoService.salvarCadastro(produto, estoqueOriginal);
        redirectAttributes.addFlashAttribute("msg", "Produto salvo com sucesso!");
        return "redirect:/admin/produtos";
      } catch (IllegalStateException | IllegalArgumentException error) {
        model.addAttribute("erro", error.getMessage());
      } catch (org.springframework.dao.DataIntegrityViolationException error) {
        model.addAttribute("erro", "Não foi possível salvar. Confira se o SKU já está em uso.");
      } catch (IOException error) {
        model.addAttribute("erro", "Não foi possível salvar a imagem.");
      }
    }
    if (produto.getId() != null) {
      Produto atual = produtoService.buscarPorId(produto.getId());
      produto.setImagemUrl(atual.getImagemUrl());
      produto.setAtivo(atual.isAtivo());
    }
    model.addAttribute("categorias", categoriaService.listarTodas());
    model.addAttribute("estoqueOriginal", estoqueOriginal);
    return "admin/produtos/form";
  }

  @PostMapping("/produtos/{id}/deletar")
  public String deletarProduto(@PathVariable Long id, RedirectAttributes flash) {
    produtoService.definirVisibilidade(id, false);
    flash.addFlashAttribute("msg", "Produto desativado.");
    return "redirect:/admin/produtos";
  }

  @PostMapping("/produtos/{id}/ativar")
  public String ativarProduto(@PathVariable Long id, RedirectAttributes flash) {
    produtoService.definirVisibilidade(id, true);
    flash.addFlashAttribute("msg", "Produto ativado.");
    return "redirect:/admin/produtos?visibilidade=todos";
  }

  // ── Categorias ─────────────────────────────────────────
  @GetMapping("/categorias")
  public String categorias(Model model) {
    model.addAttribute("categorias", categoriaService.listarResumo());
    model.addAttribute("categoria", new Categoria());
    return "admin/categorias";
  }

  @PostMapping("/categorias/salvar")
  public String salvarCategoria(
      @Valid @ModelAttribute("categoria") Categoria categoria,
      BindingResult result,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (result.hasErrors()) {
      model.addAttribute("categorias", categoriaService.listarResumo());
      return "admin/categorias";
    }
    categoriaService.salvar(categoria);
    redirectAttributes.addFlashAttribute("msg", "Categoria salva!");
    return "redirect:/admin/categorias";
  }

  @PostMapping("/categorias/{id}/deletar")
  public String deletarCategoria(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    categoriaService.deletar(id);
    redirectAttributes.addFlashAttribute("msg", "Categoria removida.");
    return "redirect:/admin/categorias";
  }

  // ── Pedidos ────────────────────────────────────────────
  @GetMapping("/pedidos")
  public String pedidos(Model model, @RequestParam(required = false) Pedido.Status status,
      @RequestParam(required = false) Pagamento.StatusPagamento pagamento, @RequestParam(defaultValue = "") String busca, @RequestParam(defaultValue = "false") boolean envio,
      @RequestParam(defaultValue = "0") int pagina) {
    var resultados = adminService.pedidos(status, pagamento, busca.strip(), envio, pagina);
    model.addAttribute("pedidos", resultados.getContent());
    model.addAttribute("pagina", resultados);
    model.addAttribute("busca", busca.strip());
    model.addAttribute("filtroStatus", status);
    model.addAttribute("filtroPagamento", pagamento);
    model.addAttribute("pagamentoOpcoes", Pagamento.StatusPagamento.values());
    model.addAttribute("envio", envio);
    model.addAttribute("statusOpcoes", Pedido.Status.values());
    return "admin/pedidos/lista";
  }

  @PostMapping("/pagamentos/conciliar")
  public String reconcile(@RequestParam String paymentId, RedirectAttributes flash) {
    if (!paymentId.matches("[0-9]{1,20}"))
      throw new IllegalArgumentException("ID de pagamento inválido.");
    try {
      payments.processarWebhook(paymentId, "payment");
      flash.addFlashAttribute("msg", "Pagamento consultado e conciliado com o provedor.");
    } catch (RuntimeException error) {
      flash.addFlashAttribute(
          "erro",
          "Não foi possível conciliar. Confira conta, valor, ambiente e configuração no provedor.");
    }
    return "redirect:/admin/pedidos";
  }

  @GetMapping("/pedidos/{id}")
  public String detalharPedido(@PathVariable Long id, Model model) {
    Pedido pedido = pedidoService.buscarPorId(id);
    model.addAttribute("pedido", pedido);
    model.addAttribute("statusOpcoes", pedidoService.acoesOperacionais(pedido));
    model.addAttribute("historico", pedidoService.historico(id));
    model.addAttribute("simulacaoDisponivel", payments.simulacaoDisponivel());
    return "admin/pedidos/detalhe";
  }

  @PostMapping("/pedidos/{id}/simular")
  public String simularPagamento(@PathVariable Long id, RedirectAttributes flash) {
    try {
      payments.simularAprovacao(id);
      flash.addFlashAttribute("msg", "Pagamento simulado. Nenhuma cobrança foi realizada.");
    } catch (IllegalStateException error) {
      flash.addFlashAttribute("erro", error.getMessage());
    }
    return "redirect:/admin/pedidos/" + id;
  }

  @PostMapping("/pedidos/{id}/status")
  public String atualizarStatus(
      @PathVariable Long id,
      @RequestParam Pedido.Status status,
      RedirectAttributes redirectAttributes) {
    try {
      pedidoService.atualizarStatusOperacional(id, status);
      redirectAttributes.addFlashAttribute("msg", "Status atualizado!");
    } catch (IllegalStateException error) {
      redirectAttributes.addFlashAttribute("erro", error.getMessage());
    }
    return "redirect:/admin/pedidos/" + id;
  }
}
