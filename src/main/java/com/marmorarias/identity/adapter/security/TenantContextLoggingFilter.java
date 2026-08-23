package com.marmorarias.identity.adapter.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Injeta org_id/user_id no MDC pra todo log da requisição carregar o tenant — ver logback-spring.xml. */
@Component
public class TenantContextLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                MDC.put("org_id", jwt.getClaimAsString("org_id"));
                MDC.put("user_id", jwt.getSubject());
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("org_id");
            MDC.remove("user_id");
        }
    }
}
