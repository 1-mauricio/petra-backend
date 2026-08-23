package com.marmorarias.billing.domain;

/** Espelha o enum installment_status do Postgres (V1). */
public enum InstallmentStatus {
    PENDENTE,
    PAGO,
    ATRASADO,
    CANCELADO
}
