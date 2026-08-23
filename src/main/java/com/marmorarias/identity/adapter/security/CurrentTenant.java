package com.marmorarias.identity.adapter.security;

import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Lê org_id/role do JWT do Supabase (claims validados via JWKS pelo resource server) para
 * a requisição corrente. Sem estado próprio — delega ao SecurityContext, já thread-bound por
 * requisição pelo Spring Security.
 */
@Component
public class CurrentTenant {

    public TenantContext get() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth)) {
            throw new IllegalStateException("Nenhum JWT autenticado no contexto atual");
        }
        Jwt jwt = auth.getToken();
        UUID organizationId = UUID.fromString(jwt.getClaimAsString("org_id"));
        UUID userId = UUID.fromString(jwt.getSubject());
        Role role = Role.valueOf(jwt.getClaimAsString("role"));
        return new TenantContext(organizationId, userId, role);
    }
}
