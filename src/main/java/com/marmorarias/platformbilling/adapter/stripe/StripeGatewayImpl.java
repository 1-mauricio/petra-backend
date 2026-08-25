package com.marmorarias.platformbilling.adapter.stripe;

import com.marmorarias.platformbilling.application.CheckoutSessionResult;
import com.marmorarias.platformbilling.application.ParsedWebhookEvent;
import com.marmorarias.platformbilling.application.StripeGateway;
import com.marmorarias.platformbilling.application.WebhookSignatureInvalidException;
import com.marmorarias.platformbilling.config.BillingProperties;
import com.marmorarias.platformbilling.domain.SubscriptionSnapshot;
import com.stripe.Stripe;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StripeGatewayImpl implements StripeGateway {

    private final BillingProperties billingProperties;

    public StripeGatewayImpl(BillingProperties billingProperties) {
        this.billingProperties = billingProperties;
    }

    @PostConstruct
    void configureApiKey() {
        Stripe.apiKey = billingProperties.secretKey();
    }

    @Override
    public CheckoutSessionResult createCheckoutSession(String existingStripeCustomerId, String priceId,
                                                         String successUrl, String cancelUrl, UUID organizationId) {
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder().setPrice(priceId).setQuantity(1L).build())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("organization_id", organizationId.toString())
                        .build());
        if (existingStripeCustomerId != null) {
            builder.setCustomer(existingStripeCustomerId);
        }

        try {
            Session session = Session.create(builder.build());
            return new CheckoutSessionResult(session.getUrl(), session.getCustomer());
        } catch (StripeException e) {
            throw new IllegalStateException("Falha ao criar checkout session no Stripe", e);
        }
    }

    @Override
    public ParsedWebhookEvent parseWebhookEvent(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, billingProperties.webhookSecret());
        } catch (SignatureVerificationException e) {
            throw new WebhookSignatureInvalidException("Assinatura Stripe inválida", e);
        }

        SubscriptionSnapshot snapshot = null;
        if (event.getType().startsWith("customer.subscription.")) {
            snapshot = extractSubscriptionSnapshot(event);
        }
        return new ParsedWebhookEvent(event.getId(), event.getType(), snapshot);
    }

    /**
     * organization_id vem do metadata setado em createCheckoutSession. Eventos sem
     * esse metadata (ex.: subscription criada fora do fluxo de checkout) são
     * ignorados — sem forma segura de resolver o tenant a partir só do evento.
     *
     * getObject() só deserializa quando a api_version do evento bate com a
     * pinada na lib (Stripe.API_VERSION); numa conta com API mais nova que o
     * SDK isso vem sempre vazio. deserializeUnsafe() ignora esse match — os
     * campos usados aqui (id, metadata, items, status) são estáveis entre
     * versões, então é seguro usar como fallback.
     */
    private SubscriptionSnapshot extractSubscriptionSnapshot(Event event) {
        StripeObject dataObject = event.getDataObjectDeserializer().getObject().orElseGet(() -> {
            try {
                return event.getDataObjectDeserializer().deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                return null;
            }
        });
        if (!(dataObject instanceof Subscription subscription)) {
            return null;
        }
        String organizationIdRaw = subscription.getMetadata() == null ? null
                : subscription.getMetadata().get("organization_id");
        if (organizationIdRaw == null) {
            return null;
        }

        SubscriptionItem firstItem = subscription.getItems().getData().get(0);
        String priceId = firstItem.getPrice().getId();
        Instant currentPeriodEnd = firstItem.getCurrentPeriodEnd() == null ? null
                : Instant.ofEpochSecond(firstItem.getCurrentPeriodEnd());

        return new SubscriptionSnapshot(UUID.fromString(organizationIdRaw), subscription.getId(),
                subscription.getCustomer(), priceId, subscription.getStatus(), currentPeriodEnd,
                billingProperties.planoForPriceId(priceId));
    }
}
