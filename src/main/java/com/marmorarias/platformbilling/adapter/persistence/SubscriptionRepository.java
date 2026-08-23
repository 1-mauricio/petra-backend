package com.marmorarias.platformbilling.adapter.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

    Optional<SubscriptionEntity> findByOrganizationId(UUID organizationId);

    Optional<SubscriptionEntity> findByStripeSubscriptionId(String stripeSubscriptionId);
}
