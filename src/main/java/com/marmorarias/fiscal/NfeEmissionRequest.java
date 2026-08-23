package com.marmorarias.fiscal;

import java.math.BigDecimal;
import java.util.UUID;

/** Formato provisório — ajustar aos campos exigidos pelo provedor escolhido (cada um modela diferente). */
public record NfeEmissionRequest(UUID orderId, UUID customerId, BigDecimal valorTotal, String descricao) {
}
