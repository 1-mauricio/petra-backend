package com.marmorarias.platformbilling.adapter.web;

import com.marmorarias.platformbilling.application.BillingService;
import com.marmorarias.platformbilling.domain.OrgBillingStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
public class CheckoutSessionController {

    private final BillingService billingService;

    public CheckoutSessionController(BillingService billingService) {
        this.billingService = billingService;
    }

    public record CheckoutSessionRequest(String plano) {
    }

    public record CheckoutSessionResponse(String url) {
    }

    public record BillingStatusResponse(OrgBillingStatus status, String plano) {
    }

    @GetMapping("/status")
    public BillingStatusResponse status(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        UUID orgId = AuthenticatedOrg.orgId(jwt);
        return new BillingStatusResponse(billingService.currentStatus(orgId), billingService.planoAtual(orgId));
    }

    @PostMapping("/checkout-session")
    @PreAuthorize("hasAnyRole('admin')")
    public CheckoutSessionResponse createCheckoutSession(@RequestBody CheckoutSessionRequest request,
                                                           JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        String url = billingService.createCheckoutSession(AuthenticatedOrg.orgId(jwt), request.plano());
        return new CheckoutSessionResponse(url);
    }
}
