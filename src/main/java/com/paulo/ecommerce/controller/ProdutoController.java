package com.paulo.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProdutoController {

    @GetMapping({"/", "/produtos", "/produtos/{id}", "/catalogo", "/produto/{id}", "/sacola", "/conta", "/conta/beneficios", "/conta/vip", "/conta/digitais", "/conta/bau/{id}/entrega", "/conta/endereco", "/checkout", "/login", "/cadastro", "/recuperar-senha", "/redefinir-senha", "/pagamento/retorno", "/passe", "/passe/missoes"})
    public String storefront() {
        return "forward:/index.html";
    }
    @GetMapping("/eventos")
    public String eventos() {
        return "redirect:/passe#eventos";
    }
}
