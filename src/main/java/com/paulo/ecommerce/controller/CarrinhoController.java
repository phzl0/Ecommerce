package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.dto.CheckoutForm;
import com.paulo.ecommerce.service.CarrinhoService;
import com.paulo.ecommerce.service.PedidoService;
import com.paulo.ecommerce.service.ProdutoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/carrinho")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;
    private final ProdutoService produtoService;
    private final PedidoService pedidoService;

    public CarrinhoController(CarrinhoService carrinhoService,
                              ProdutoService produtoService,
                              PedidoService pedidoService) {
        this.carrinhoService = carrinhoService;
        this.produtoService = produtoService;
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public String verCarrinho(HttpSession session, Model model) {
        popularResumo(session, model);
        return "carrinho/carrinho";
    }

    @PostMapping("/adicionar")
    @ResponseBody
    public String adicionar(@RequestParam Long produtoId,
                            @RequestParam(defaultValue = "1") int quantidade,
                            HttpSession session) {
        try {
            var produto = produtoService.buscarPorId(produtoId);
            carrinhoService.adicionar(session, produto, quantidade);
            return "{\"sucesso\":true,\"total\":" +
                    carrinhoService.getTotalItens(session) + "}";
        } catch (Exception e) {
            return "{\"sucesso\":false,\"erro\":\"" + e.getMessage() + "\"}";
        }
    }

    @PostMapping("/remover")
    public String remover(@RequestParam Long produtoId, HttpSession session) {
        carrinhoService.remover(session, produtoId);
        return "redirect:/carrinho";
    }

    @PostMapping("/atualizar")
    public String atualizar(@RequestParam Long produtoId,
                            @RequestParam int quantidade,
                            HttpSession session) {
        carrinhoService.atualizarQuantidade(session, produtoId, quantidade);
        return "redirect:/carrinho";
    }

    @GetMapping("/checkout")
    public String checkout(HttpSession session, Model model) {
        if (carrinhoService.getCarrinho(session).isEmpty()) {
            return "redirect:/carrinho";
        }
        if (!model.containsAttribute("checkoutForm")) {
            model.addAttribute("checkoutForm", new CheckoutForm());
        }
        popularResumo(session, model);
        return "carrinho/checkout";
    }

    @PostMapping("/checkout")
    public String finalizarPedido(@Valid CheckoutForm checkoutForm,
                                  BindingResult result,
                                  HttpSession session,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("checkoutForm", checkoutForm);
            popularResumo(session, model);
            return "carrinho/checkout";
        }

        try {
            var pedido = pedidoService.finalizarPedido(
                    userDetails.getUsername(), checkoutForm, session);
            redirectAttributes.addFlashAttribute("msg",
                    "Pedido " + pedido.getNumero() + " realizado com sucesso!");
            return "redirect:/pagamento/pedido/" + pedido.getId();
        } catch (RuntimeException e) {
            model.addAttribute("checkoutForm", checkoutForm);
            model.addAttribute("erro", e.getMessage());
            popularResumo(session, model);
            return "carrinho/checkout";
        }
    }

    private void popularResumo(HttpSession session, Model model) {
        model.addAttribute("itens", carrinhoService.getCarrinho(session));
        model.addAttribute("subtotal", carrinhoService.getSubtotal(session));
        model.addAttribute("frete", carrinhoService.calcularFrete(session));
        model.addAttribute("desconto", carrinhoService.getDesconto(session));
        model.addAttribute("total", carrinhoService.getTotal(session));
        model.addAttribute("faltaFreteGratis", carrinhoService.getFaltaParaFreteGratis(session));
    }
}
