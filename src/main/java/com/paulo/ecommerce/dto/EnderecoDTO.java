package com.paulo.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EnderecoDTO {

    private String cep;
    private String logradouro;
    private String complemento;
    private String bairro;
    private String localidade;
    private String uf;
    private boolean erro;

    @JsonProperty("cep")
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    @JsonProperty("logradouro")
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    @JsonProperty("complemento")
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    @JsonProperty("bairro")
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    @JsonProperty("localidade")
    public String getLocalidade() { return localidade; }
    public void setLocalidade(String localidade) { this.localidade = localidade; }

    @JsonProperty("uf")
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public boolean isErro() { return erro; }
    public void setErro(boolean erro) { this.erro = erro; }

    public String getEnderecoFormatado() {
        if (erro) return null;
        return String.format("%s, %s, %s — %s/%s",
                logradouro, bairro, complemento, localidade, uf);
    }
}