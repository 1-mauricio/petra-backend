package com.marmorarias.platformbilling.domain;

/** Espelha o enum org_billing_status do Postgres (V13). */
public enum OrgBillingStatus {
    SEM_ASSINATURA,
    ATIVA,
    INADIMPLENTE,
    CANCELADA
}
