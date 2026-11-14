package com.paulo.ecommerce.model;
import jakarta.persistence.*;
@Entity
@Table(name="digital_config")
public class DigitalConfig {
 @Id public Long produtoId;
 @Column(nullable=false,length=20) public String modo;
 public Long arquivoId;
}
