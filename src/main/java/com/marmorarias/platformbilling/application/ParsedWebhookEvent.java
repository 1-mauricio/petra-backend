package com.marmorarias.platformbilling.application;

import com.marmorarias.platformbilling.domain.SubscriptionSnapshot;

/**
 * subscriptionSnapshot é null quando o evento não é de ciclo de vida de
 * subscription, ou quando não carrega organization_id no metadata (evento
 * ignorado, mas ainda registrado para idempotência).
 */
public record ParsedWebhookEvent(String eventId, String eventType, SubscriptionSnapshot subscriptionSnapshot) {
}
