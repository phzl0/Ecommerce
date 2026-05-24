package com.paulo.ecommerce.service;

import com.paulo.ecommerce.dto.EnderecoDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CepService {

    private static final String VIACEP_URL =
            "https://viacep.com.br/ws/{cep}/json/";

    private final RestTemplate restTemplate;

    public CepService() {
        this.restTemplate = new RestTemplate();
    }

    public EnderecoDTO buscarCep(String cep) {
        String cepLimpo = cep.replaceAll("[^0-9]", "");

        if (cepLimpo.length() != 8) {
            EnderecoDTO erro = new EnderecoDTO();
            erro.setErro(true);
            return erro;
        }

        try {
            EnderecoDTO endereco = restTemplate.getForObject(
                    VIACEP_URL, EnderecoDTO.class, cepLimpo);
            return endereco != null ? endereco : new EnderecoDTO();
        } catch (Exception e) {
            EnderecoDTO erro = new EnderecoDTO();
            erro.setErro(true);
            return erro;
        }
    }
}