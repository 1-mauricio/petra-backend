package com.marmorarias.identity.adapter.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Ponte entre o admin da plataforma e o bypass de RLS (V27): sem organização própria, precisa
 * de uma GUC separada de app.current_org_id para ler/escrever entre tenants. Mesma regra de
 * RlsContext — sempre como primeira linha do método @Transactional — e nunca chamado fora de
 * @PreAuthorize("hasRole('platform_admin')").
 */
@Component
public class PlatformRlsContext {

    @PersistenceContext
    private EntityManager entityManager;

    /** Leitura/escrita cross-org (ex.: listar todas as organizações). */
    public void enablePlatformScope() {
        entityManager.createNativeQuery("SELECT set_config('app.is_platform_admin', 'true', true)").getSingleResult();
    }

    /** Escrita dirigida a uma única org (ex.: convidar usuário numa org específica). */
    public void scopeToOrg(UUID organizationId) {
        entityManager.createNativeQuery("SELECT set_config('app.current_org_id', ?1, true)")
                .setParameter(1, organizationId.toString())
                .getSingleResult();
    }
}
