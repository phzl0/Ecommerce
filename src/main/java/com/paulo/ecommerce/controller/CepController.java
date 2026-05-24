package com.paulo.ecommerce.controller;

import com.paulo.ecommerce.dto.EnderecoDTO;
import com.paulo.ecommerce.service.CepService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cep")
public class CepController {

    private final CepService cepService;

    public CepController(CepService cepService) {
        this.cepService = cepService;
    }

    @GetMapping("/{cep}")
    public ResponseEntity<EnderecoDTO> buscar(@PathVariable String cep) {
        EnderecoDTO endereco = cepService.buscarCep(cep);
        if (endereco.isErro()) {
            return ResponseEntity.badRequest().body(endereco);
        }
        return ResponseEntity.ok(endereco);
    }
}