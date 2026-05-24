package com.paulo.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProdutoController {

    @GetMapping({"/", "/produtos", "/produtos/{id}", "/catalogo", "/produto/{id}", "/sacola", "/conta", "/checkout", "/login", "/cadastro", "/pagamento/retorno", "/passe", "/passe/missoes"})
    public String storefront() {
        return "forward:/index.html";
    }
}
