package com.marmorarias.platformbilling.adapter.persistence;

import com.marmorarias.platformbilling.domain.OrgBillingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Mapeia só as colunas de billing da tabela organization (id, stripe_customer_id,
 * billing_status). A entidade completa de organization pertence a outro
 * contexto quando existir; este módulo nunca insere linhas aqui, só lê/atualiza.
 */
@Entity
@Table(name = "organization")
public class OrganizationBillingEntity {

    @Id
    private UUID id;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "billing_status", nullable = false)
    private OrgBillingStatus billingStatus;

    protected OrganizationBillingEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public OrgBillingStatus getBillingStatus() {
        return billingStatus;
    }

    public void setBillingStatus(OrgBillingStatus billingStatus) {
        this.billingStatus = billingStatus;
    }
}
