package com.marmorarias.quoting.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "quote")
public class QuoteEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected QuoteEntity() {
    }

    public QuoteEntity(UUID organizationId, UUID customerId, UUID leadId) {
        this.organizationId = organizationId;
        this.customerId = customerId;
        this.leadId = leadId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getLeadId() {
        return leadId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
