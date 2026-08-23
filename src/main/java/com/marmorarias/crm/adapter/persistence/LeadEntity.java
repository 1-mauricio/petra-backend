package com.marmorarias.crm.adapter.persistence;

import com.marmorarias.crm.domain.LeadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead")
public class LeadEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "customer_id")
    private UUID customerId;

    private String origem;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private LeadStatus status;

    @Column(name = "motivo_perda")
    private String motivoPerda;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LeadEntity() {
    }

    public LeadEntity(UUID organizationId, UUID customerId, String origem) {
        this.organizationId = organizationId;
        this.customerId = customerId;
        this.origem = origem;
        this.status = LeadStatus.ABERTO;
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

    public String getOrigem() {
        return origem;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public String getMotivoPerda() {
        return motivoPerda;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void marcarStatus(LeadStatus novoStatus) {
        this.status = novoStatus;
    }

    public void marcarPerdido(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("motivo é obrigatório ao marcar lead como PERDIDO");
        }
        this.status = LeadStatus.PERDIDO;
        this.motivoPerda = motivo;
    }

    public void marcarGanho() {
        this.status = LeadStatus.GANHO;
    }
}
