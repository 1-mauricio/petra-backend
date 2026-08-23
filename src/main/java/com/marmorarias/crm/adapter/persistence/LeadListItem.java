package com.marmorarias.crm.adapter.persistence;

import com.marmorarias.crm.domain.LeadStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LeadListItem(
        UUID id,
        UUID customerId,
        String customerNome,
        String origem,
        LeadStatus status,
        String motivoPerda,
        OffsetDateTime createdAt) {
}
