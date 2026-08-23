package com.marmorarias.orders.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.marmorarias.orders.domain.OrderState.*;

/** Função pura: só sabe quais transições de estágio são topologicamente permitidas. */
public final class OrderStateMachine {

    private static final Map<OrderState, Set<OrderState>> TRANSICOES = new EnumMap<>(OrderState.class);

    static {
        TRANSICOES.put(ORCAMENTO, EnumSet.of(APROVACAO, CANCELADO));
        TRANSICOES.put(APROVACAO, EnumSet.of(PEDIDO, CANCELADO));
        TRANSICOES.put(PEDIDO, EnumSet.of(LEVANTAMENTO_TECNICO, CANCELADO));
        TRANSICOES.put(LEVANTAMENTO_TECNICO, EnumSet.of(REVISAO_ORCAMENTO, PRODUCAO, CANCELADO));
        TRANSICOES.put(REVISAO_ORCAMENTO, EnumSet.of(PRODUCAO, CANCELADO));
        TRANSICOES.put(PRODUCAO, EnumSet.of(ENTREGA, CANCELADO));
        TRANSICOES.put(ENTREGA, EnumSet.of(INSTALACAO, CANCELADO));
        TRANSICOES.put(INSTALACAO, EnumSet.of(CONCLUIDO, CANCELADO));
        TRANSICOES.put(CONCLUIDO, EnumSet.noneOf(OrderState.class));
        TRANSICOES.put(CANCELADO, EnumSet.noneOf(OrderState.class));
    }

    private OrderStateMachine() {
    }

    public static boolean permitida(OrderState de, OrderState para) {
        return TRANSICOES.getOrDefault(de, Set.of()).contains(para);
    }

    public static Set<OrderState> proximosEstados(OrderState de) {
        return TRANSICOES.getOrDefault(de, Set.of());
    }
}
