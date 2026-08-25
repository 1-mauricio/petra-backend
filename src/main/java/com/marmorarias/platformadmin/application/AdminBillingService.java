package com.marmorarias.platformadmin.application;

import com.marmorarias.identity.adapter.persistence.PlatformRlsContext;
import com.marmorarias.platformadmin.adapter.persistence.OrganizationEntity;
import com.marmorarias.platformadmin.adapter.persistence.OrganizationRepository;
import com.marmorarias.platformbilling.adapter.persistence.SubscriptionEntity;
import com.marmorarias.platformbilling.adapter.persistence.SubscriptionRepository;
import com.marmorarias.platformbilling.adapter.persistence.WebhookEventEntity;
import com.marmorarias.platformbilling.adapter.persistence.WebhookEventRepository;
import com.marmorarias.platformbilling.domain.OrgBillingStatus;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Espelho do billing entre organizações + override manual de status/plano (courtesy access,
 * regularização de suporte) — sem disparar operação real no Stripe; reembolso/cancelamento de
 * verdade seguem pelo dashboard Stripe, fora do escopo deste painel.
 */
@Service
public class AdminBillingService {

    private static final Logger log = LoggerFactory.getLogger(AdminBillingService.class);

    private final PlatformRlsContext platformRlsContext;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WebhookEventRepository webhookEventRepository;

    public AdminBillingService(PlatformRlsContext platformRlsContext, OrganizationRepository organizationRepository,
                                SubscriptionRepository subscriptionRepository,
                                WebhookEventRepository webhookEventRepository) {
        this.platformRlsContext = platformRlsContext;
        this.organizationRepository = organizationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.webhookEventRepository = webhookEventRepository;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionEntity> listarAssinaturas() {
        platformRlsContext.enablePlatformScope();
        return subscriptionRepository.findAll();
    }

    /** billing_webhook_event não é org-scoped (evento chega antes de qualquer RLS setada) — feed único, sem filtro por organização. */
    @Transactional(readOnly = true)
    public List<WebhookEventEntity> listarEventosRecentes() {
        return webhookEventRepository.findAll(Sort.by(Sort.Direction.DESC, "processedAt"));
    }

    /** plano sem subscription real ainda gera uma linha sintética (stripe_status="admin_override") — só pra refletir o override, nunca sincronizada por webhook. */
    @Transactional
    public OrganizationEntity sobrescrever(UUID adminUserId, UUID orgId, OrgBillingStatus billingStatus, String plano) {
        platformRlsContext.enablePlatformScope();
        OrganizationEntity org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organização não encontrada: " + orgId));
        if (billingStatus != null) {
            org.atualizar(null, null, billingStatus);
        }
        if (plano != null) {
            SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(orgId)
                    .orElseGet(() -> new SubscriptionEntity(orgId, "admin_override_" + orgId, "admin_override",
                            "admin_override", null, plano));
            subscription.setPlano(plano);
            subscriptionRepository.save(subscription);
        }
        log.info("platform_admin={} sobrescreveu billing organizacao={} status={} plano={}", adminUserId, orgId,
                billingStatus, plano);
        return org;
    }
}
