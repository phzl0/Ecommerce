package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.dto.api.PasseAcaoRequest;
import com.paulo.ecommerce.dto.api.PasseClaimResponse;
import com.paulo.ecommerce.dto.api.PasseDashboardResponse;
import com.paulo.ecommerce.model.PasseAcaoTipo;
import com.paulo.ecommerce.service.PasseService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pass")
public class PasseApiController {

    @org.springframework.beans.factory.annotation.Autowired private com.paulo.ecommerce.service.PagamentoService pagamentos;
    private final PasseService passeService;

    public PasseApiController(PasseService passeService) {
        this.passeService = passeService;
    }

    @GetMapping("/current")
    public PasseDashboardResponse current(@AuthenticationPrincipal UserDetails userDetails) {
        return passeService.carregarDashboard(userDetails.getUsername());
    }

    @PostMapping("/rewards/{id}/apply")
    public void apply(@PathVariable Long id,@AuthenticationPrincipal UserDetails user){passeService.aplicarTema(user.getUsername(),id);}
    @GetMapping("/rewards")
    public java.util.List<java.util.Map<String,Object>> rewards(@AuthenticationPrincipal UserDetails user) { return passeService.colecao(user.getUsername()); }
    @GetMapping("/rewards/{id}/download")
    public org.springframework.http.ResponseEntity<byte[]> download(@PathVariable Long id,@AuthenticationPrincipal UserDetails user) {
        return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=level-beneficio-"+id+".svg").contentType(org.springframework.http.MediaType.valueOf("image/svg+xml")).body(passeService.baixarBeneficio(user.getUsername(),id));
    }

    @PostMapping("/premium")
    public com.paulo.ecommerce.dto.api.PaymentRedirectResponse premium(@AuthenticationPrincipal UserDetails user) {
        Long id=passeService.criarPedidoPremium(user.getUsername());
        return new com.paulo.ecommerce.dto.api.PaymentRedirectResponse(pagamentos.iniciarFluxoPagamento(id,com.paulo.ecommerce.model.Pagamento.MetodoPagamento.PIX));
    }

    @PostMapping("/actions")
    public void action(@RequestBody PasseAcaoRequest request,
                       @AuthenticationPrincipal UserDetails userDetails) {
        PasseAcaoTipo acaoTipo = PasseAcaoTipo.valueOf(request.acao());
        if (acaoTipo != PasseAcaoTipo.CHECKIN && acaoTipo != PasseAcaoTipo.VIEW_PRODUCT && acaoTipo != PasseAcaoTipo.ADD_TO_CART) {
            throw new IllegalArgumentException("Esta ação só pode ser confirmada pelo servidor.");
        }
        if (request.valor() != null && request.valor() != 1) throw new IllegalArgumentException("A quantidade desta ação deve ser 1.");
        int valor = 1;
        passeService.registrarAcaoNavegador(userDetails.getUsername(), acaoTipo, request.referencia());
    }

    @PostMapping("/rewards/{rewardId}/claim")
    public PasseClaimResponse claimReward(@PathVariable Long rewardId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        return passeService.reclamarRecompensa(userDetails.getUsername(), rewardId);
    }
}
