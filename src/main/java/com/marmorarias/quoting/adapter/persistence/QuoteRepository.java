package com.marmorarias.quoting.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<QuoteEntity, UUID> {

    List<QuoteEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<QuoteEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
