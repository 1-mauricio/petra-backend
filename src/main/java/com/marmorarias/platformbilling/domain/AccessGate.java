package com.marmorarias.platformbilling.domain;

/**
 * Decide se uma requisição de escrita pode prosseguir dado o status de billing
 * da organização. Modo restrito: apenas ATIVA libera escrita; leitura (GET)
 * nunca é bloqueada. Sem período de trial no MVP — SEM_ASSINATURA também restringe.
 */
public final class AccessGate {

    private AccessGate() {
    }

    public static boolean isWriteAllowed(OrgBillingStatus status) {
        return status == OrgBillingStatus.ATIVA;
    }
}
