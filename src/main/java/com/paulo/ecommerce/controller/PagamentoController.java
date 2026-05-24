package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.service.PagamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pagamento")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @GetMapping("/pedido/{pedidoId}")
    public String paginaPagamento(@PathVariable Long pedidoId) {
        return "redirect:/pedidos/" + pedidoId + "?openPayment=true";
    }

    @PostMapping("/iniciar/{pedidoId}")
    public String iniciarPagamento(@PathVariable Long pedidoId,
                                   RedirectAttributes redirectAttributes) {
        try {
            String checkoutUrl =
                    pagamentoService.criarPreferencia(pedidoId);
            return "redirect:" + checkoutUrl;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro",
                    "Erro ao iniciar pagamento. Tente novamente.");
            return "redirect:/pedidos/" + pedidoId + "?paymentError=true";
        }
    }

    @GetMapping("/sucesso")
    public String sucesso(@RequestParam(required = false) String preference_id) {
        Long pedidoId = null;
        if (preference_id != null) {
            pagamentoService.atualizarStatusManual(preference_id, "approved");
            try {
                var pagamento =
                        pagamentoService.buscarPorPedido(
                                pagamentoService.buscarPorPreferenceId(preference_id)
                                        .getPedido().getId());
                pedidoId = pagamento.getPedido().getId();
            } catch (Exception ignored) {}
        }
        return buildPaymentRedirect("sucesso", preference_id, pedidoId);
    }

    @GetMapping("/falha")
    public String falha(@RequestParam(required = false) String preference_id) {
        Long pedidoId = null;
        if (preference_id != null) {
            pagamentoService.atualizarStatusManual(preference_id, "failure");
            try {
                pedidoId = pagamentoService.buscarPorPreferenceId(preference_id)
                        .getPedido().getId();
            } catch (Exception ignored) {}
        }
        return buildPaymentRedirect("falha", preference_id, pedidoId);
    }

    @GetMapping("/pendente")
    public String pendente(@RequestParam(required = false) String preference_id) {
        Long pedidoId = null;
        if (preference_id != null) {
            pagamentoService.atualizarStatusManual(preference_id, "pending");
            try {
                pedidoId = pagamentoService.buscarPorPreferenceId(preference_id)
                        .getPedido().getId();
            } catch (Exception ignored) {}
        }
        return buildPaymentRedirect("pendente", preference_id, pedidoId);
    }

    // Webhook do Mercado Pago — deve ser POST público
    @PostMapping("/webhook")
    @ResponseBody
    public ResponseEntity<Void> webhook(
            @RequestParam(required = false) String type,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestBody(required = false) String body) {
        try {
            if ("payment".equals(type) && dataId != null) {
                pagamentoService.processarWebhook(dataId, type);
            }
        } catch (Exception e) {
            System.err.println("Webhook error: " + e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    private String buildPaymentRedirect(String status, String preferenceId, Long pedidoId) {
        StringBuilder redirect = new StringBuilder("redirect:/pagamento/retorno?status=")
                .append(status);
        if (preferenceId != null && !preferenceId.isBlank()) {
            redirect.append("&preferenceId=").append(preferenceId);
        }
        if (pedidoId != null) {
            redirect.append("&pedidoId=").append(pedidoId);
        }
        return redirect.toString();
    }
}
