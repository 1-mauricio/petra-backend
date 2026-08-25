package com.marmorarias.platformbilling.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription")
public class SubscriptionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "stripe_subscription_id", nullable = false)
    private String stripeSubscriptionId;

    @Column(name = "stripe_price_id", nullable = false)
    private String stripePriceId;

    @Column(name = "stripe_status", nullable = false)
    private String stripeStatus;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "plano", nullable = false)
    private String plano;

    protected SubscriptionEntity() {
    }

    public SubscriptionEntity(UUID organizationId, String stripeSubscriptionId, String stripePriceId,
                               String stripeStatus, Instant currentPeriodEnd, String plano) {
        this.organizationId = organizationId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripePriceId = stripePriceId;
        this.stripeStatus = stripeStatus;
        this.currentPeriodEnd = currentPeriodEnd;
        this.plano = plano;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public String getStripePriceId() {
        return stripePriceId;
    }

    public void setStripePriceId(String stripePriceId) {
        this.stripePriceId = stripePriceId;
    }

    public String getStripeStatus() {
        return stripeStatus;
    }

    public void setStripeStatus(String stripeStatus) {
        this.stripeStatus = stripeStatus;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public String getPlano() {
        return plano;
    }

    public void setPlano(String plano) {
        this.plano = plano;
    }
}
