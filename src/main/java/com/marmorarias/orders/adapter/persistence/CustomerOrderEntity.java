package com.marmorarias.orders.adapter.persistence;

import com.marmorarias.orders.domain.OrderState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** "order" é palavra reservada em SQL — a entidade PEDIDO vive na tabela customer_order (V6). */
@Entity
@Table(name = "customer_order")
public class CustomerOrderEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "current_quote_version_id", nullable = false)
    private UUID currentQuoteVersionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private OrderState state;

    @Column(name = "valor_taxa_cancelamento")
    private BigDecimal valorTaxaCancelamento;

    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected CustomerOrderEntity() {
    }

    public CustomerOrderEntity(UUID organizationId, UUID customerId, UUID currentQuoteVersionId) {
        this.organizationId = organizationId;
        this.customerId = customerId;
        this.currentQuoteVersionId = currentQuoteVersionId;
        this.state = OrderState.ORCAMENTO;
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

    public UUID getCurrentQuoteVersionId() {
        return currentQuoteVersionId;
    }

    public void apontarNovaVersao(UUID quoteVersionId) {
        this.currentQuoteVersionId = quoteVersionId;
    }

    public OrderState getState() {
        return state;
    }

    public void aplicarTransicao(OrderState novoEstado) {
        this.state = novoEstado;
    }

    public BigDecimal getValorTaxaCancelamento() {
        return valorTaxaCancelamento;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void registrarCancelamento(BigDecimal taxaMedicao, String motivo) {
        this.valorTaxaCancelamento = taxaMedicao;
        this.motivoCancelamento = motivo;
    }
}
