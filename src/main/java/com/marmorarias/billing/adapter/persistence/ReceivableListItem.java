package com.marmorarias.billing.adapter.persistence;

import com.marmorarias.billing.domain.InstallmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReceivableListItem(UUID id, UUID orderId, BigDecimal valorTotal, List<Parcela> installments) {

    public record Parcela(UUID id, UUID receivableId, int numero, BigDecimal valor, LocalDate vencimento,
                           InstallmentStatus status) {
    }
}
