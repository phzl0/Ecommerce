package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.dto.api.AuthStatusResponse;
import com.paulo.ecommerce.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UsuarioService usuarioService;

    public AuthApiController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public AuthStatusResponse me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return new AuthStatusResponse(false, null, null, null, null);
        }

        var usuario = usuarioService.buscarPorEmail(userDetails.getUsername());
        return new AuthStatusResponse(
                true,
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTemaPasse(),
                usuario.getRole().name()
        );
    }
}
