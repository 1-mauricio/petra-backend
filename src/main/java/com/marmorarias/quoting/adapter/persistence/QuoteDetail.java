package com.marmorarias.quoting.adapter.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record QuoteDetail(
        UUID id,
        UUID customerId,
        String customerNome,
        UUID leadId,
        OffsetDateTime createdAt,
        List<QuoteVersionDetail> versions) {
}
