package com.marmorarias.platformbilling.application;

import com.marmorarias.platformbilling.adapter.persistence.OrganizationBillingEntity;
import com.marmorarias.platformbilling.adapter.persistence.OrganizationBillingRepository;
import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.platformbilling.adapter.persistence.SubscriptionEntity;
import com.marmorarias.platformbilling.adapter.persistence.SubscriptionRepository;
import com.marmorarias.platformbilling.adapter.persistence.WebhookEventEntity;
import com.marmorarias.platformbilling.adapter.persistence.WebhookEventRepository;
import com.marmorarias.platformbilling.config.BillingProperties;
import com.marmorarias.platformbilling.domain.BillingStatusMapper;
import com.marmorarias.platformbilling.domain.OrgBillingStatus;
import com.marmorarias.platformbilling.domain.SubscriptionSnapshot;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private final StripeGateway stripeGateway;
    private final BillingProperties billingProperties;
    private final RlsContext rlsContext;
    private final OrganizationBillingRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WebhookEventRepository webhookEventRepository;

    public BillingService(StripeGateway stripeGateway, BillingProperties billingProperties, RlsContext rlsContext,
                           OrganizationBillingRepository organizationRepository,
                           SubscriptionRepository subscriptionRepository,
                           WebhookEventRepository webhookEventRepository) {
        this.stripeGateway = stripeGateway;
        this.billingProperties = billingProperties;
        this.rlsContext = rlsContext;
        this.organizationRepository = organizationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.webhookEventRepository = webhookEventRepository;
    }

    @Transactional
    public String createCheckoutSession(UUID organizationId, String plano) {
        rlsContext.setCurrentOrg(organizationId);
        OrganizationBillingEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organização não encontrada: " + organizationId));

        String priceId = billingProperties.priceIdForPlano(plano);
        CheckoutSessionResult result = stripeGateway.createCheckoutSession(org.getStripeCustomerId(), priceId,
                billingProperties.successUrl(), billingProperties.cancelUrl(), organizationId);

        if (org.getStripeCustomerId() == null) {
            org.setStripeCustomerId(result.stripeCustomerId());
        }
        return result.url();
    }

    @Transactional(readOnly = true)
    public OrgBillingStatus currentStatus(UUID organizationId) {
        rlsContext.setCurrentOrg(organizationId);
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organização não encontrada: " + organizationId))
                .getBillingStatus();
    }

    /**
     * Idempotente por event_id: se já processado, retorna sem reaplicar efeito.
     * Assinatura inválida propaga (o controller responde 400, sem gravar nada).
     */
    @Transactional
    public void handleWebhookEvent(String payload, String signatureHeader) {
        ParsedWebhookEvent event = stripeGateway.parseWebhookEvent(payload, signatureHeader);

        if (webhookEventRepository.existsById(event.eventId())) {
            return;
        }

        SubscriptionSnapshot snapshot = event.subscriptionSnapshot();
        if (snapshot != null) {
            applySubscriptionSnapshot(snapshot);
        }

        webhookEventRepository.save(new WebhookEventEntity(event.eventId(), event.eventType()));
    }

    private void applySubscriptionSnapshot(SubscriptionSnapshot snapshot) {
        rlsContext.setCurrentOrg(snapshot.organizationId());

        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(snapshot.organizationId())
                .orElse(null);
        if (subscription == null) {
            subscription = new SubscriptionEntity(snapshot.organizationId(), snapshot.stripeSubscriptionId(),
                    snapshot.stripePriceId(), snapshot.stripeStatus(), snapshot.currentPeriodEnd());
        } else {
            subscription.setStripePriceId(snapshot.stripePriceId());
            subscription.setStripeStatus(snapshot.stripeStatus());
            subscription.setCurrentPeriodEnd(snapshot.currentPeriodEnd());
        }
        subscriptionRepository.save(subscription);

        OrganizationBillingEntity org = organizationRepository.findById(snapshot.organizationId())
                .orElseThrow(() -> new NoSuchElementException("Organização não encontrada: " + snapshot.organizationId()));
        org.setBillingStatus(BillingStatusMapper.fromStripeStatus(snapshot.stripeStatus()));
        if (org.getStripeCustomerId() == null) {
            org.setStripeCustomerId(snapshot.stripeCustomerId());
        }
    }
}
