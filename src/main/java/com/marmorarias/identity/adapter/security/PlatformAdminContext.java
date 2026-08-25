package com.marmorarias.identity.adapter.security;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Identidade do admin da plataforma (staff Petra, sem organização própria — ver V27/V28) para a
 * requisição corrente. Paralelo a CurrentTenant, mas sem org_id/role: quem chama já passou por
 * @PreAuthorize("hasRole('platform_admin')"), então só precisamos do próprio id pra auditoria.
 */
@Component
public class PlatformAdminContext {

    public UUID userId() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth)) {
            throw new IllegalStateException("Nenhum JWT autenticado no contexto atual");
        }
        return UUID.fromString(auth.getToken().getSubject());
    }
}
