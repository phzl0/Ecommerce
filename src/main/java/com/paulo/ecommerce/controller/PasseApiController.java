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
        passeService.registrarAcao(userDetails.getUsername(), PasseAcaoTipo.CHECKIN, 1, "dashboard");
        return passeService.carregarDashboard(userDetails.getUsername());
    }

    @PostMapping("/actions")
    public void action(@RequestBody PasseAcaoRequest request,
                       @AuthenticationPrincipal UserDetails userDetails) {
        PasseAcaoTipo acaoTipo = PasseAcaoTipo.valueOf(request.acao());
        int valor = request.valor() == null ? 1 : request.valor();
        passeService.registrarAcao(userDetails.getUsername(), acaoTipo, valor, request.referencia());
    }

    @PostMapping("/rewards/{rewardId}/claim")
    public PasseClaimResponse claimReward(@PathVariable Long rewardId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        return passeService.reclamarRecompensa(userDetails.getUsername(), rewardId);
    }
}
