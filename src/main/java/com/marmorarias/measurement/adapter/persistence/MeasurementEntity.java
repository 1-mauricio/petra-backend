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

/**
 * Fluxo de campo: AGENDADO -> EM_CAMPO -> CONCLUIDO -> APROVADO/REJEITADO (V23). Captura direta
 * (peças já vêm junto na criação, sem visita agendada) pula direto para CONCLUIDO. Imutável após
 * APROVADO (invariante 5) — reforçado por trigger no banco (V7).
 */
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

    @Column(name = "data_agendada")
    private Instant dataAgendada;

    @Column(name = "data_medicao", insertable = false, updatable = false)
    private Instant dataMedicao;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected MeasurementEntity() {
    }

    /** Captura direta (dados já coletados na hora) — sem passar por agendamento. */
    public MeasurementEntity(UUID organizationId, UUID orderId, UUID tecnicoResponsavel) {
        this.organizationId = organizationId;
        this.orderId = orderId;
        this.tecnicoResponsavel = tecnicoResponsavel;
        this.status = MeasurementStatus.CONCLUIDO;
    }

    /** Visita agendada, sem peças ainda — segue para EM_CAMPO e depois CONCLUIDO. */
    public MeasurementEntity(UUID organizationId, UUID orderId, UUID tecnicoResponsavel, Instant dataAgendada) {
        this.organizationId = organizationId;
        this.orderId = orderId;
        this.tecnicoResponsavel = tecnicoResponsavel;
        this.dataAgendada = dataAgendada;
        this.status = MeasurementStatus.AGENDADO;
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

    public Instant getDataAgendada() {
        return dataAgendada;
    }

    public Instant getDataMedicao() {
        return dataMedicao;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void iniciarCampo() {
        if (status != MeasurementStatus.AGENDADO) {
            throw new IllegalStateException("measurement " + id + " não pode iniciar campo a partir de " + status);
        }
        this.status = MeasurementStatus.EM_CAMPO;
    }

    public void concluir() {
        if (status != MeasurementStatus.AGENDADO && status != MeasurementStatus.EM_CAMPO) {
            throw new IllegalStateException("measurement " + id + " não pode ser concluído a partir de " + status);
        }
        this.status = MeasurementStatus.CONCLUIDO;
    }

    public void aprovar() {
        if (status != MeasurementStatus.CONCLUIDO) {
            throw new IllegalStateException("measurement " + id + " não pode ser aprovado a partir de " + status);
        }
        this.status = MeasurementStatus.APROVADO;
        this.approvedAt = Instant.now();
    }

    public void rejeitar() {
        if (status != MeasurementStatus.CONCLUIDO) {
            throw new IllegalStateException("measurement " + id + " não pode ser rejeitado a partir de " + status);
        }
        this.status = MeasurementStatus.REJEITADO;
    }
}
