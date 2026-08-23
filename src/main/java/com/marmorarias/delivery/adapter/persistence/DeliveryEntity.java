package com.marmorarias.delivery.adapter.persistence;

import com.marmorarias.delivery.domain.DeliveryStatus;
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

@Entity
@Table(name = "delivery")
public class DeliveryEntity {

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
    private DeliveryStatus status;

    @Column(name = "data_agendada")
    private Instant dataAgendada;

    @Column(name = "data_entrega")
    private Instant dataEntrega;

    @Column(name = "comprovante_url")
    private String comprovanteUrl;

    protected DeliveryEntity() {
    }

    public DeliveryEntity(UUID organizationId, UUID orderId, Instant dataAgendada) {
        this.organizationId = organizationId;
        this.orderId = orderId;
        this.dataAgendada = dataAgendada;
        this.status = DeliveryStatus.AGENDADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public Instant getDataAgendada() {
        return dataAgendada;
    }

    public Instant getDataEntrega() {
        return dataEntrega;
    }

    public String getComprovanteUrl() {
        return comprovanteUrl;
    }

    public void marcarEntregue() {
        this.status = DeliveryStatus.ENTREGUE;
        this.dataEntrega = Instant.now();
    }

    public void registrarComprovante(String url) {
        this.comprovanteUrl = url;
        marcarEntregue();
    }

    public void atualizarStatus(DeliveryStatus status) {
        this.status = status;
    }
}
