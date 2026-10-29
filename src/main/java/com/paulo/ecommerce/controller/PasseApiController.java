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

    private final PasseService passeService;

    public PasseApiController(PasseService passeService) {
        this.passeService = passeService;
    }

    @GetMapping("/current")
    public PasseDashboardResponse current(@AuthenticationPrincipal UserDetails userDetails) {
        return passeService.carregarDashboard(userDetails.getUsername());
    }

    @PostMapping("/actions")
    public void action(@RequestBody PasseAcaoRequest request,
                       @AuthenticationPrincipal UserDetails userDetails) {
        PasseAcaoTipo acaoTipo = PasseAcaoTipo.valueOf(request.acao());
        if (acaoTipo != PasseAcaoTipo.CHECKIN && acaoTipo != PasseAcaoTipo.VIEW_PRODUCT && acaoTipo != PasseAcaoTipo.ADD_TO_CART) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Esta ação só pode ser confirmada pelo servidor.");
        }
        if (request.valor() != null && request.valor() != 1) throw new IllegalArgumentException("A quantidade desta ação deve ser 1.");
        int valor = 1;
        passeService.registrarAcao(userDetails.getUsername(), acaoTipo, valor, request.referencia());
    }

    @PostMapping("/rewards/{rewardId}/claim")
    public PasseClaimResponse claimReward(@PathVariable Long rewardId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        return passeService.reclamarRecompensa(userDetails.getUsername(), rewardId);
    }
}
