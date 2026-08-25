package com.marmorarias.platformadmin.adapter.web;

import com.marmorarias.identity.adapter.security.PlatformAdminContext;
import com.marmorarias.platformadmin.adapter.persistence.OrganizationEntity;
import com.marmorarias.platformadmin.application.AdminBillingService;
import com.marmorarias.platformbilling.adapter.persistence.SubscriptionEntity;
import com.marmorarias.platformbilling.adapter.persistence.WebhookEventEntity;
import com.marmorarias.platformbilling.domain.OrgBillingStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Espelho de billing entre organizações + override manual (courtesy access/regularização de
 * suporte). Nenhuma operação real de dinheiro no Stripe aqui — reembolso/cancelamento seguem
 * pelo dashboard Stripe.
 */
@RestController
@RequestMapping("/api/admin/billing")
@PreAuthorize("hasRole('platform_admin')")
public class AdminBillingController {

    private final AdminBillingService adminBillingService;
    private final PlatformAdminContext platformAdminContext;

    public AdminBillingController(AdminBillingService adminBillingService, PlatformAdminContext platformAdminContext) {
        this.adminBillingService = adminBillingService;
        this.platformAdminContext = platformAdminContext;
    }

    public record SobrescreverBillingRequest(OrgBillingStatus billingStatus, String plano) {
    }

    @GetMapping("/assinaturas")
    public List<SubscriptionEntity> listarAssinaturas() {
        return adminBillingService.listarAssinaturas();
    }

    @GetMapping("/eventos")
    public List<WebhookEventEntity> listarEventos() {
        return adminBillingService.listarEventosRecentes();
    }

    @PatchMapping("/organizacoes/{orgId}")
    public OrganizationEntity sobrescrever(@PathVariable UUID orgId, @RequestBody SobrescreverBillingRequest request) {
        return adminBillingService.sobrescrever(platformAdminContext.userId(), orgId, request.billingStatus(),
                request.plano());
    }
}
