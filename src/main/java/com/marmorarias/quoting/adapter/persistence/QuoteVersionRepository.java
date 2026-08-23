package com.marmorarias.quoting.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteVersionRepository extends JpaRepository<QuoteVersionEntity, UUID> {

    List<QuoteVersionEntity> findByQuoteIdOrderByVersionNumberDesc(UUID quoteId);

    Optional<QuoteVersionEntity> findTopByQuoteIdOrderByVersionNumberDesc(UUID quoteId);
}
