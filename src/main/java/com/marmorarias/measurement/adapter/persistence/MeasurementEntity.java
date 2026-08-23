package com.marmorarias.measurement.adapter.persistence;

import com.marmorarias.measurement.domain.MeasurementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Imutável após APROVADO (invariante 5) — reforçado por trigger no banco (V7). */
@Entity
@Table(name = "measurement")
public class MeasurementEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private MeasurementStatus status;

    @Column(name = "tecnico_responsavel")
    private UUID tecnicoResponsavel;

    @Column(name = "data_medicao", insertable = false, updatable = false)
    private Instant dataMedicao;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected MeasurementEntity() {
    }

    public MeasurementEntity(UUID organizationId, UUID orderId, UUID tecnicoResponsavel) {
        this.organizationId = organizationId;
        this.orderId = orderId;
        this.tecnicoResponsavel = tecnicoResponsavel;
        this.status = MeasurementStatus.PENDENTE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public MeasurementStatus getStatus() {
        return status;
    }

    public UUID getTecnicoResponsavel() {
        return tecnicoResponsavel;
    }

    public Instant getDataMedicao() {
        return dataMedicao;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void aprovar() {
        if (status != MeasurementStatus.PENDENTE) {
            throw new IllegalStateException("measurement " + id + " não pode ser aprovado a partir de " + status);
        }
        this.status = MeasurementStatus.APROVADO;
        this.approvedAt = Instant.now();
    }

    public void rejeitar() {
        if (status != MeasurementStatus.PENDENTE) {
            throw new IllegalStateException("measurement " + id + " não pode ser rejeitado a partir de " + status);
        }
        this.status = MeasurementStatus.REJEITADO;
    }
}
