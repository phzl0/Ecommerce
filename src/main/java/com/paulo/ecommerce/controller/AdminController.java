package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.service.*;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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

  // ── Dashboard ──────────────────────────────────────────
  @GetMapping
  public String dashboard(Model model) {
    model.addAttribute("totalPedidos", adminService.totalPedidos());
    model.addAttribute("totalProdutos", adminService.totalProdutos());
    model.addAttribute("totalClientes", adminService.totalClientes());
    model.addAttribute("receitaTotal", adminService.receitaTotal());
    model.addAttribute("pedidosRecentes", adminService.pedidosRecentes());
    model.addAttribute("produtosSemEstoque", adminService.produtosSemEstoque());
    model.addAttribute("pedidosPendentes", adminService.pedidosPendentes());
    return "admin/dashboard";
  }

  // ── Produtos ───────────────────────────────────────────
  @GetMapping("/produtos")
  public String produtos(Model model) {
    model.addAttribute("produtos", produtoService.listarAtivos(0, 100).getContent());
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
    model.addAttribute("produto", produtoService.buscarPorId(id));
    model.addAttribute("categorias", categoriaService.listarTodas());
    return "admin/produtos/form";
  }

  @PostMapping("/produtos/salvar")
  public String salvarProduto(
      @Valid @ModelAttribute("produto") Produto produto,
      BindingResult result,
      @RequestParam(required = false) MultipartFile imagem,
      @RequestParam(required = false) Long categoriaId,
      Model model,
      RedirectAttributes redirectAttributes)
      throws IOException {
    if (result.hasErrors()) {
      model.addAttribute("categorias", categoriaService.listarTodas());
      return "admin/produtos/form";
    }

    if (categoriaId != null) {
      produto.setCategoria(categoriaService.buscarPorId(categoriaId));
    }

    if (imagem != null && !imagem.isEmpty()) {
      produto.setImagemUrl(imageStorage.save(imagem));
    }

    produtoService.salvar(produto);
    redirectAttributes.addFlashAttribute("msg", "Produto salvo com sucesso!");
    return "redirect:/admin/produtos";
  }

  @PostMapping("/produtos/{id}/deletar")
  public String deletarProduto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    produtoService.deletar(id);
    redirectAttributes.addFlashAttribute("msg", "Produto removido.");
    return "redirect:/admin/produtos";
  }

  // ── Categorias ─────────────────────────────────────────
  @GetMapping("/categorias")
  public String categorias(Model model) {
    model.addAttribute("categorias", categoriaService.listarTodas());
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
      model.addAttribute("categorias", categoriaService.listarTodas());
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
  public String pedidos(Model model) {
    model.addAttribute("pedidos", pedidoService.listarTodos());
    model.addAttribute("status", Pedido.Status.values());
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
    model.addAttribute("pedido", pedidoService.buscarPorId(id));
    model.addAttribute("statusOpcoes", Pedido.Status.values());
    return "admin/pedidos/detalhe";
  }

  @PostMapping("/pedidos/{id}/status")
  public String atualizarStatus(
      @PathVariable Long id,
      @RequestParam Pedido.Status status,
      RedirectAttributes redirectAttributes) {
    pedidoService.atualizarStatus(id, status);
    redirectAttributes.addFlashAttribute("msg", "Status atualizado!");
    return "redirect:/admin/pedidos/" + id;
  }
}
