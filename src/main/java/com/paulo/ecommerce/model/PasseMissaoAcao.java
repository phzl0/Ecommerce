package com.paulo.ecommerce.model;
import jakarta.persistence.*;
@Entity @Table(name="passe_missao_acoes",uniqueConstraints=@UniqueConstraint(columnNames={"usuario_id","template_id","periodo_inicio","referencia"}))
public class PasseMissaoAcao {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long usuarioId;
 @Column(nullable=false) public Long templateId;
 @Column(nullable=false) public java.time.LocalDateTime periodoInicio;
 @Column(nullable=false,length=180) public String referencia;
}
