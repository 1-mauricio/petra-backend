package com.marmorarias.platformbilling.application;

import java.util.UUID;

/** Porta para o adapter Stripe — o núcleo não conhece o SDK do Stripe. */
public interface StripeGateway {

    CheckoutSessionResult createCheckoutSession(String existingStripeCustomerId, String priceId, String successUrl,
                                                 String cancelUrl, UUID organizationId);

    ParsedWebhookEvent parseWebhookEvent(String payload, String signatureHeader) throws WebhookSignatureInvalidException;
}
