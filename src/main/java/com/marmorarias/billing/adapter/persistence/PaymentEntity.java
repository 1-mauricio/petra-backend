package com.marmorarias.billing.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class PaymentEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "installment_id", nullable = false)
    private UUID installmentId;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "data_pagamento", nullable = false)
    private Instant dataPagamento;

    @Column(name = "forma_pagamento", nullable = false)
    private String formaPagamento;

    protected PaymentEntity() {
    }

    public PaymentEntity(UUID organizationId, UUID installmentId, BigDecimal valor, String formaPagamento) {
        this.organizationId = organizationId;
        this.installmentId = installmentId;
        this.valor = valor;
        this.formaPagamento = formaPagamento;
        this.dataPagamento = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstallmentId() {
        return installmentId;
    }

    public BigDecimal getValor() {
        return valor;
    }
}
