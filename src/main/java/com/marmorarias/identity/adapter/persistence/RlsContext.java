package com.marmorarias.identity.adapter.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Ponte entre o org_id resolvido (JWT ou, no webhook do Stripe, metadata do evento) e a RLS
 * do Postgres. Precisa rodar dentro da mesma transação/conexão das queries seguintes — chamar
 * sempre como primeira linha do método @Transactional.
 */
@Component
public class RlsContext {

    @PersistenceContext
    private EntityManager entityManager;

    public void setCurrentOrg(UUID organizationId) {
        entityManager.createNativeQuery("SELECT set_config('app.current_org_id', ?1, true)")
                .setParameter(1, organizationId.toString())
                .getSingleResult();
    }
}
