package com.paulo.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    @GetMapping
    public String meusPedidos() {
        return "forward:/index.html";
    }

    @GetMapping("/{id}")
    public String detalhe() {
        return "forward:/index.html";
    }
}
