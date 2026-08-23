package com.marmorarias.quoting.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteLineItemRepository extends JpaRepository<QuoteLineItemEntity, UUID> {

    List<QuoteLineItemEntity> findByQuoteVersionId(UUID quoteVersionId);
}
