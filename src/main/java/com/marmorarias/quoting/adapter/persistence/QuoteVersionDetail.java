package com.marmorarias.quoting.adapter.persistence;

import com.marmorarias.quoting.domain.QuoteVersionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuoteVersionDetail(
        UUID id,
        UUID quoteId,
        int versionNumber,
        QuoteVersionStatus status,
        BigDecimal valorTotal,
        Instant createdAt,
        Instant approvedAt,
        List<QuoteLineItemEntity> lineItems) {
}
