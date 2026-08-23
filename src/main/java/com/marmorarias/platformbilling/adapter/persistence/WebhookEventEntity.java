package com.marmorarias.platformbilling.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Registro append-only de eventos de webhook do Stripe já processados (idempotência). */
@Entity
@Table(name = "billing_webhook_event")
public class WebhookEventEntity {

    @Id
    @Column(name = "stripe_event_id")
    private String stripeEventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected WebhookEventEntity() {
    }

    public WebhookEventEntity(String stripeEventId, String eventType) {
        this.stripeEventId = stripeEventId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public String getEventType() {
        return eventType;
    }
}
