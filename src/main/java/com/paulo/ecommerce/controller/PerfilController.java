package com.paulo.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    @GetMapping
    public String perfil() {
        return "forward:/index.html";
    }
}
