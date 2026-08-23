package com.marmorarias.delivery.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, UUID> {

    List<DeliveryEntity> findByOrderId(UUID orderId);

    List<DeliveryEntity> findByOrganizationId(UUID organizationId);
}
