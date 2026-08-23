package com.marmorarias.billing.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "receivable")
public class ReceivableEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    protected ReceivableEntity() {
    }

    public ReceivableEntity(UUID organizationId, UUID orderId, BigDecimal valorTotal) {
        this.organizationId = organizationId;
        this.orderId = orderId;
        this.valorTotal = valorTotal;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }
}
