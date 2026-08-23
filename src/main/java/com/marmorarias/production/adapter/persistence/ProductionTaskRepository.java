package com.marmorarias.production.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionTaskRepository extends JpaRepository<ProductionTaskEntity, UUID> {

    List<ProductionTaskEntity> findByOrderId(UUID orderId);

    List<ProductionTaskEntity> findByOrganizationId(UUID organizationId);
}
