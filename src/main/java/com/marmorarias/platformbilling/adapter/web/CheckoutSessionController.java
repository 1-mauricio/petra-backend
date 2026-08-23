package com.marmorarias.platformbilling.adapter.web;

import com.marmorarias.platformbilling.application.BillingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/checkout-session")
    @PreAuthorize("hasRole('ADMIN')")
    public CheckoutSessionResponse createCheckoutSession(@RequestBody CheckoutSessionRequest request,
                                                           JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        String url = billingService.createCheckoutSession(AuthenticatedOrg.orgId(jwt), request.plano());
        return new CheckoutSessionResponse(url);
    }
}
