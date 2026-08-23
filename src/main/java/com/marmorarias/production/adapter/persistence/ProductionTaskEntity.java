package com.marmorarias.production.adapter.persistence;

import com.marmorarias.production.domain.ProductionTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "production_task")
public class ProductionTaskEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ProductionTaskStatus status;

    private UUID responsavel;

    protected ProductionTaskEntity() {
    }

    public ProductionTaskEntity(UUID organizationId, UUID orderId, String descricao, UUID responsavel) {
        this.organizationId = organizationId;
        this.orderId = orderId;
        this.descricao = descricao;
        this.responsavel = responsavel;
        this.status = ProductionTaskStatus.PENDENTE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getDescricao() {
        return descricao;
    }

    public ProductionTaskStatus getStatus() {
        return status;
    }

    public UUID getResponsavel() {
        return responsavel;
    }

    public void atualizarStatus(ProductionTaskStatus novoStatus) {
        this.status = novoStatus;
    }
}
