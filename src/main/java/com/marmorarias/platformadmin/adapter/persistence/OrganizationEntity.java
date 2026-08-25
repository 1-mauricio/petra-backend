package com.marmorarias.platformadmin.adapter.persistence;

import com.marmorarias.platformbilling.domain.OrgBillingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Visão completa de organization — só o admin da plataforma precisa de todas as colunas de
 * uma vez. Outros módulos usam views parciais da mesma tabela (ex.: platformbilling's
 * OrganizationBillingEntity, que mapeia só id/stripe_customer_id/billing_status).
 */
@Entity
@Table(name = "organization")
public class OrganizationEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String nome;

    private String cnpj;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "billing_status", nullable = false)
    private OrgBillingStatus billingStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrganizationEntity() {
    }

    public OrganizationEntity(String nome, String cnpj) {
        this.nome = nome;
        this.cnpj = cnpj;
        this.billingStatus = OrgBillingStatus.SEM_ASSINATURA;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public OrgBillingStatus getBillingStatus() {
        return billingStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void atualizar(String nome, String cnpj, OrgBillingStatus billingStatus) {
        if (nome != null) {
            this.nome = nome;
        }
        if (cnpj != null) {
            this.cnpj = cnpj;
        }
        if (billingStatus != null) {
            this.billingStatus = billingStatus;
        }
    }
}
