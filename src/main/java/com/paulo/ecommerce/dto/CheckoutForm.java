package com.paulo.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CheckoutForm {
  @Size(max = 60)
  private String cupom;

  public String getCupom() {
    return cupom;
  }

  public void setCupom(String value) {
    cupom = value;
  }

  @NotBlank(message = "Informe o nome de quem vai receber.")
  @Size(max = 120, message = "O nome do destinatario deve ter ate 120 caracteres.")
  private String nomeDestinatario;

  @NotBlank(message = "Informe um telefone para contato.")
  @Size(max = 20, message = "Telefone invalido.")
  private String telefoneContato;

  @NotBlank(message = "Informe o CEP.")
  @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP invalido.")
  private String cep;

  @NotBlank(message = "Informe o endereco.")
  @Size(max = 160, message = "Endereco muito longo.")
  private String logradouro;

  @NotBlank(message = "Informe o numero.")
  @Size(max = 20, message = "Numero invalido.")
  private String numero;

  @Size(max = 80, message = "Complemento muito longo.")
  private String complemento;

  @NotBlank(message = "Informe o bairro.")
  @Size(max = 80, message = "Bairro muito longo.")
  private String bairro;

  @NotBlank(message = "Informe a cidade.")
  @Size(max = 80, message = "Cidade muito longa.")
  private String cidade;

  @NotBlank(message = "Informe o estado.")
  @Size(min = 2, max = 2, message = "Use a sigla do estado.")
  private String uf;

  @Size(max = 300, message = "Observacao muito longa.")
  private String observacoes;

  public String getEnderecoCompleto() {
    StringBuilder builder = new StringBuilder();
    builder.append(logradouro).append(", ").append(numero);
    if (complemento != null && !complemento.isBlank()) {
      builder.append(" - ").append(complemento.trim());
    }
    builder
        .append(", ")
        .append(bairro)
        .append(", ")
        .append(cidade)
        .append(" - ")
        .append(uf)
        .append(", CEP ")
        .append(cep);
    return builder.toString();
  }

  public String getNomeDestinatario() {
    return nomeDestinatario;
  }

  public void setNomeDestinatario(String nomeDestinatario) {
    this.nomeDestinatario = nomeDestinatario;
  }

  public String getTelefoneContato() {
    return telefoneContato;
  }

  public void setTelefoneContato(String telefoneContato) {
    this.telefoneContato = telefoneContato;
  }

  public String getCep() {
    return cep;
  }

  public void setCep(String cep) {
    this.cep = cep;
  }

  public String getLogradouro() {
    return logradouro;
  }

  public void setLogradouro(String logradouro) {
    this.logradouro = logradouro;
  }

  public String getNumero() {
    return numero;
  }

  public void setNumero(String numero) {
    this.numero = numero;
  }

  public String getComplemento() {
    return complemento;
  }

  public void setComplemento(String complemento) {
    this.complemento = complemento;
  }

  public String getBairro() {
    return bairro;
  }

  public void setBairro(String bairro) {
    this.bairro = bairro;
  }

  public String getCidade() {
    return cidade;
  }

  public void setCidade(String cidade) {
    this.cidade = cidade;
  }

  public String getUf() {
    return uf;
  }

  public void setUf(String uf) {
    this.uf = uf;
  }

  public String getObservacoes() {
    return observacoes;
  }

  public void setObservacoes(String observacoes) {
    this.observacoes = observacoes;
  }
}
