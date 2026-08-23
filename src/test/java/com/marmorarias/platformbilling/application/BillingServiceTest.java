package com.marmorarias.platformbilling.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marmorarias.platformbilling.adapter.persistence.OrganizationBillingEntity;
import com.marmorarias.platformbilling.adapter.persistence.OrganizationBillingRepository;
import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.platformbilling.adapter.persistence.SubscriptionRepository;
import com.marmorarias.platformbilling.adapter.persistence.WebhookEventRepository;
import com.marmorarias.platformbilling.config.BillingProperties;
import com.marmorarias.platformbilling.domain.SubscriptionSnapshot;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BillingServiceTest {

    private final StripeGateway stripeGateway = mock(StripeGateway.class);
    private final RlsContext rlsContext = mock(RlsContext.class);
    private final OrganizationBillingRepository organizationRepository = mock(OrganizationBillingRepository.class);
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final WebhookEventRepository webhookEventRepository = mock(WebhookEventRepository.class);
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        BillingProperties billingProperties = new BillingProperties("sk_test", "whsec_test", "https://ok",
                "https://cancel", Map.of(), null);
        billingService = new BillingService(stripeGateway, billingProperties, rlsContext, organizationRepository,
                subscriptionRepository, webhookEventRepository);
    }

    @Test
    void webhookIdempotente_mesmoEventIdDuasVezes_umEfeito() throws Exception {
        UUID orgId = UUID.randomUUID();
        SubscriptionSnapshot snapshot = new SubscriptionSnapshot(orgId, "sub_1", "cus_1", "price_1", "active",
                Instant.now());
        ParsedWebhookEvent event = new ParsedWebhookEvent("evt_1", "customer.subscription.updated", snapshot);
        when(stripeGateway.parseWebhookEvent(any(), any())).thenReturn(event);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(newOrg(orgId)));
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.empty());

        billingService.handleWebhookEvent("payload", "sig");
        markEventAsRecorded("evt_1");

        billingService.handleWebhookEvent("payload", "sig");

        verify(subscriptionRepository, times(1)).save(any());
        verify(organizationRepository, times(1)).findById(orgId);
    }

    @Test
    void assinaturaInvalidaERejeitada() {
        when(stripeGateway.parseWebhookEvent(any(), any()))
                .thenThrow(new WebhookSignatureInvalidException("inválida", null));

        assertThrows(WebhookSignatureInvalidException.class,
                () -> billingService.handleWebhookEvent("payload", "sig-ruim"));
        verify(webhookEventRepository, times(0)).save(any());
    }

    @Test
    void overrideLocalFixaPlanoEStatusSemConsultarBanco() {
        BillingProperties comOverride = new BillingProperties("sk_test", "whsec_test", "https://ok",
                "https://cancel", Map.of(), "pro");
        BillingService comOverrideService = new BillingService(stripeGateway, comOverride, rlsContext,
                organizationRepository, subscriptionRepository, webhookEventRepository);

        UUID orgId = UUID.randomUUID();
        org.junit.jupiter.api.Assertions.assertEquals("pro", comOverrideService.planoAtual(orgId));
        org.junit.jupiter.api.Assertions.assertEquals(
                com.marmorarias.platformbilling.domain.OrgBillingStatus.ATIVA, comOverrideService.currentStatus(orgId));
        verify(organizationRepository, times(0)).findById(any());
        verify(subscriptionRepository, times(0)).findByOrganizationId(any());
    }

    private void markEventAsRecorded(String eventId) {
        when(webhookEventRepository.existsById(eventId)).thenReturn(true);
    }

    private OrganizationBillingEntity newOrg(UUID id) throws Exception {
        Constructor<OrganizationBillingEntity> constructor = OrganizationBillingEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        OrganizationBillingEntity org = constructor.newInstance();
        Field idField = OrganizationBillingEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(org, id);
        return org;
    }
}
