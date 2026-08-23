package com.marmorarias.orders.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderStateMachineTest {

    @Test
    void pipelineFelizSeguePassoAPasso() {
        assertTrue(OrderStateMachine.permitida(OrderState.ORCAMENTO, OrderState.APROVACAO));
        assertTrue(OrderStateMachine.permitida(OrderState.APROVACAO, OrderState.PEDIDO));
        assertTrue(OrderStateMachine.permitida(OrderState.PEDIDO, OrderState.LEVANTAMENTO_TECNICO));
        assertTrue(OrderStateMachine.permitida(OrderState.LEVANTAMENTO_TECNICO, OrderState.PRODUCAO));
        assertTrue(OrderStateMachine.permitida(OrderState.LEVANTAMENTO_TECNICO, OrderState.REVISAO_ORCAMENTO));
        assertTrue(OrderStateMachine.permitida(OrderState.REVISAO_ORCAMENTO, OrderState.PRODUCAO));
        assertTrue(OrderStateMachine.permitida(OrderState.PRODUCAO, OrderState.ENTREGA));
        assertTrue(OrderStateMachine.permitida(OrderState.ENTREGA, OrderState.INSTALACAO));
        assertTrue(OrderStateMachine.permitida(OrderState.INSTALACAO, OrderState.CONCLUIDO));
    }

    @Test
    void naoPulaEtapas() {
        assertFalse(OrderStateMachine.permitida(OrderState.ORCAMENTO, OrderState.PRODUCAO));
        assertFalse(OrderStateMachine.permitida(OrderState.ORCAMENTO, OrderState.PEDIDO));
        assertFalse(OrderStateMachine.permitida(OrderState.PEDIDO, OrderState.PRODUCAO));
    }

    @Test
    void cancelamentoAlcancavelDaMaioriaDosEstados() {
        for (OrderState estado : OrderState.values()) {
            if (estado == OrderState.CONCLUIDO || estado == OrderState.CANCELADO) {
                continue;
            }
            assertTrue(OrderStateMachine.permitida(estado, OrderState.CANCELADO),
                    "CANCELADO deveria ser alcançável a partir de " + estado);
        }
    }

    @Test
    void estadosTerminaisNaoTransicionamParaMaisNada() {
        for (OrderState alvo : OrderState.values()) {
            assertFalse(OrderStateMachine.permitida(OrderState.CONCLUIDO, alvo));
            assertFalse(OrderStateMachine.permitida(OrderState.CANCELADO, alvo));
        }
    }
}
