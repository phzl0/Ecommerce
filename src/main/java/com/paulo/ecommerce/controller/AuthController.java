package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/index")
    public String home() {
        return "redirect:/";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String erro,
                            @RequestParam(required = false) String logout,
                            @RequestParam(required = false) String returnUrl) {
        StringBuilder redirect = new StringBuilder("redirect:/login");
        if (erro != null) {
            redirect.append("?erro=true");
        } else if (logout != null) {
            redirect.append("?logout=true");
        }
        if (returnUrl != null && returnUrl.startsWith("/")) {
            redirect.append(redirect.indexOf("?") >= 0 ? "&" : "?")
                    .append("returnUrl=")
                    .append(java.net.URLEncoder.encode(returnUrl, java.nio.charset.StandardCharsets.UTF_8));
        }
        return redirect.toString();
    }

    @GetMapping("/cadastro")
    public String cadastroPage() {
        return "redirect:/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("usuario") Usuario usuario,
                            BindingResult result) {
        if (result.hasErrors()) {
            return "redirect:/cadastro?erro=true";
        }
        try {
            usuarioService.cadastrar(usuario);
            return "redirect:/login?sucesso=true";
        } catch (RuntimeException e) {
            return "redirect:/cadastro?erro=true";
        }
    }
}
