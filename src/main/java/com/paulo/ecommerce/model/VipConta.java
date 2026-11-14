package com.paulo.ecommerce.model;

import jakarta.persistence.*;

@Entity
@Table(name="vip_contas")
public class VipConta {
  @Id public Long usuarioId;
  public java.time.LocalDateTime cicloInicio = java.time.LocalDateTime.now();
  public java.time.LocalDateTime ultimaAtividade;
  public boolean expirado;
}
