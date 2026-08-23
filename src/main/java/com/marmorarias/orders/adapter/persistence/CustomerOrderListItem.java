package com.marmorarias.orders.adapter.persistence;

import com.marmorarias.orders.domain.OrderState;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Projeção de listagem: pedido + nome do cliente, sem trazer a entidade Customer inteira. */
public record CustomerOrderListItem(
        UUID id,
        UUID customerId,
        String customerNome,
        UUID currentQuoteVersionId,
        OrderState state,
        BigDecimal valorTaxaCancelamento,
        String motivoCancelamento,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
