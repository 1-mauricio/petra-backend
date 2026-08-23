package com.marmorarias.platformbilling.adapter.web;

import com.marmorarias.platformbilling.application.BillingService;
import com.marmorarias.platformbilling.domain.AccessGate;
import com.marmorarias.platformbilling.domain.OrgBillingStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Modo restrito: org sem assinatura ATIVA só pode fazer leitura (GET) e usar
 * os próprios endpoints de billing (para poder assinar/regularizar). Demais
 * escritas levam 402. Ver CLAUDE.md/platform_billing — "definir o que bloquear".
 */
@Component
public class BillingAccessGateFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final BillingService billingService;

    public BillingAccessGateFilter(BillingService billingService) {
        this.billingService = billingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SAFE_METHODS.contains(request.getMethod()) || request.getRequestURI().startsWith("/api/billing")) {
            chain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof Jwt jwt)) {
            chain.doFilter(request, response);
            return;
        }

        OrgBillingStatus status = billingService.currentStatus(AuthenticatedOrg.orgId(jwt));
        if (!AccessGate.isWriteAllowed(status)) {
            response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
            response.setContentType("application/json");
            response.getWriter().write("{\"erro\":\"assinatura_inativa\",\"billingStatus\":\"" + status + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
