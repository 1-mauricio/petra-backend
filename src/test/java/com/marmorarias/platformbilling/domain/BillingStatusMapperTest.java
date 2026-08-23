package com.marmorarias.platformbilling.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BillingStatusMapperTest {

    @Test
    void ativaParaStatusAtivoOuTrial() {
        assertEquals(OrgBillingStatus.ATIVA, BillingStatusMapper.fromStripeStatus("active"));
        assertEquals(OrgBillingStatus.ATIVA, BillingStatusMapper.fromStripeStatus("trialing"));
    }

    @Test
    void inadimplenteParaPagamentoEmAtraso() {
        assertEquals(OrgBillingStatus.INADIMPLENTE, BillingStatusMapper.fromStripeStatus("past_due"));
        assertEquals(OrgBillingStatus.INADIMPLENTE, BillingStatusMapper.fromStripeStatus("unpaid"));
        assertEquals(OrgBillingStatus.INADIMPLENTE, BillingStatusMapper.fromStripeStatus("incomplete"));
    }

    @Test
    void canceladaParaStatusEncerrado() {
        assertEquals(OrgBillingStatus.CANCELADA, BillingStatusMapper.fromStripeStatus("canceled"));
        assertEquals(OrgBillingStatus.CANCELADA, BillingStatusMapper.fromStripeStatus("incomplete_expired"));
    }

    @Test
    void statusDesconhecidoFalhaFechado() {
        assertEquals(OrgBillingStatus.INADIMPLENTE, BillingStatusMapper.fromStripeStatus("algo_novo_do_stripe"));
        assertEquals(OrgBillingStatus.INADIMPLENTE, BillingStatusMapper.fromStripeStatus(null));
    }
}
