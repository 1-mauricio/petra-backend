package com.marmorarias.delivery.domain;

/** Espelha o enum delivery_status do Postgres (V1). */
public enum DeliveryStatus {
    AGENDADA,
    EM_ROTA,
    ENTREGUE,
    CANCELADA
}
