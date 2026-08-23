package com.marmorarias.quoting.adapter.persistence;

import com.marmorarias.quoting.domain.QuoteVersionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record QuoteSummary(
        UUID id,
        String customerNome,
        int latestVersionNumber,
        QuoteVersionStatus latestVersionStatus,
        BigDecimal valorTotal,
        OffsetDateTime createdAt) {
}
