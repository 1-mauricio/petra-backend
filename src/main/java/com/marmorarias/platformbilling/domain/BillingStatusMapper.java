package com.marmorarias.platformbilling.domain;

import java.util.Set;

/**
 * Mapeia o status bruto da subscription do Stripe para o status de billing da
 * organização. Função pura — sem I/O, sem SDK do Stripe — para poder ser
 * testada sem mocks e para o núcleo não depender do adapter externo.
 * Status desconhecido cai em INADIMPLENTE (fail-closed: restringe em vez de liberar).
 */
public final class BillingStatusMapper {

    private static final Set<String> ATIVA = Set.of("active", "trialing");
    private static final Set<String> CANCELADA = Set.of("canceled", "incomplete_expired", "paused");

    private BillingStatusMapper() {
    }

    public static OrgBillingStatus fromStripeStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return OrgBillingStatus.INADIMPLENTE;
        }
        if (ATIVA.contains(stripeStatus)) {
            return OrgBillingStatus.ATIVA;
        }
        if (CANCELADA.contains(stripeStatus)) {
            return OrgBillingStatus.CANCELADA;
        }
        return OrgBillingStatus.INADIMPLENTE;
    }
}
