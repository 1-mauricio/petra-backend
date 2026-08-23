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
import java.util.UUID;

/** Append-only (grants revogam UPDATE/DELETE para app_user — V11): histórico de transições. */
@Entity
@Table(name = "stage_transition")
public class StageTransitionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "from_state")
    private OrderState fromState;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "to_state", nullable = false)
    private OrderState toState;

    private UUID actor;

    private String motivo;

    protected StageTransitionEntity() {
    }

    public StageTransitionEntity(UUID organizationId, UUID orderId, OrderState fromState, OrderState toState,
                                  UUID actor, String motivo) {
        this.organizationId = organizationId;
        this.orderId = orderId;
        this.fromState = fromState;
        this.toState = toState;
        this.actor = actor;
        this.motivo = motivo;
    }
}
