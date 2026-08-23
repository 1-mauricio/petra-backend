package com.marmorarias.billing.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableRepository extends JpaRepository<ReceivableEntity, UUID> {

    List<ReceivableEntity> findByOrderId(UUID orderId);

    List<ReceivableEntity> findByOrganizationId(UUID organizationId);
}
