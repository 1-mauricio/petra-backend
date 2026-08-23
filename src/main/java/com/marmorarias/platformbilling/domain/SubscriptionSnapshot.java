package com.marmorarias.platformbilling.domain;

import java.time.Instant;
import java.util.UUID;

/** Estado de uma subscription do Stripe já traduzido para o vocabulário do núcleo. */
public record SubscriptionSnapshot(UUID organizationId, String stripeSubscriptionId, String stripeCustomerId,
                                    String stripePriceId, String stripeStatus, Instant currentPeriodEnd) {
}
