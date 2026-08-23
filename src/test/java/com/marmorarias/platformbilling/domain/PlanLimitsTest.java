package com.marmorarias.platformbilling.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlanLimitsTest {

    @Test
    void basicoBloqueiaNoLimiteDeUsuarios() {
        assertTrue(PlanLimits.usuarioDentroDoLimite("basico", 2));
        assertFalse(PlanLimits.usuarioDentroDoLimite("basico", 3));
    }

    @Test
    void basicoBloqueiaNoLimiteDePedidos() {
        assertTrue(PlanLimits.pedidoDentroDoLimite("basico", 49));
        assertFalse(PlanLimits.pedidoDentroDoLimite("basico", 50));
    }

    @Test
    void proNuncaBloqueia() {
        assertTrue(PlanLimits.usuarioDentroDoLimite("pro", 1_000));
        assertTrue(PlanLimits.pedidoDentroDoLimite("pro", 1_000));
    }

    @Test
    void planoDesconhecidoCaiNoLimiteMaisRestrito() {
        assertFalse(PlanLimits.usuarioDentroDoLimite("inexistente", 3));
    }
}
