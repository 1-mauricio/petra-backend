package com.marmorarias.orders.domain;

/** Espelha o enum order_state do Postgres (V1) — pipeline central do pedido. */
public enum OrderState {
    ORCAMENTO,
    APROVACAO,
    PEDIDO,
    LEVANTAMENTO_TECNICO,
    REVISAO_ORCAMENTO,
    PRODUCAO,
    ENTREGA,
    INSTALACAO,
    CONCLUIDO,
    CANCELADO
}
