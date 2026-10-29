package com.paulo.ecommerce.dto.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CheckoutRequest {
  @Size(max = 60)
  private String cupom;

  public String getCupom() {
    return cupom;
  }

  public void setCupom(String value) {
    cupom = value;
  }

  @NotBlank
  @Pattern(regexp = "[a-zA-Z0-9-]{16,64}")
  private String idempotencyKey;

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String value) {
    idempotencyKey = value;
  }

  @NotBlank
  @Size(max = 120)
  private String nome;

  @NotBlank
  @Size(max = 20)
  private String telefone;

  @NotBlank
  @Pattern(regexp = "\\d{5}-?\\d{3}")
  private String cep;

  @NotBlank
  @Size(max = 160)
  private String endereco;

  @NotBlank
  @Size(max = 20)
  private String numero;

  @Size(max = 80)
  private String complemento;

  @NotBlank
  @Size(max = 80)
  private String bairro;

  @NotBlank
  @Size(max = 80)
  private String cidade;

  @NotBlank
  @Size(min = 2, max = 2)
  private String estado;

  @NotBlank
  @Pattern(regexp = "pix|boleto|cartao")
  private String pagamento;

  @Valid
  @NotEmpty
  @Size(max = 100)
  private List<CheckoutItemRequest> itens;

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getTelefone() {
    return telefone;
  }

  public void setTelefone(String telefone) {
    this.telefone = telefone;
  }

  public String getCep() {
    return cep;
  }

  public void setCep(String cep) {
    this.cep = cep;
  }

  public String getEndereco() {
    return endereco;
  }

  public void setEndereco(String endereco) {
    this.endereco = endereco;
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

  public String getEstado() {
    return estado;
  }

  public void setEstado(String estado) {
    this.estado = estado;
  }

  public String getPagamento() {
    return pagamento;
  }

  public void setPagamento(String pagamento) {
    this.pagamento = pagamento;
  }

  public List<CheckoutItemRequest> getItens() {
    return itens;
  }

  public void setItens(List<CheckoutItemRequest> itens) {
    this.itens = itens;
  }
}
