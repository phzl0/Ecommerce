package com.paulo.ecommerce.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    private LocalDateTime atualizadoEm;

    @CreatedBy
    @Column(updatable = false)
    private String criadoPor;

    @LastModifiedBy
    private String atualizadoPor;

    public LocalDateTime getCriadoEm()     { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public String getCriadoPor()           { return criadoPor; }
    public String getAtualizadoPor()       { return atualizadoPor; }
}