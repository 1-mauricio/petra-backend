package com.marmorarias.orders.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageTransitionRepository extends JpaRepository<StageTransitionEntity, UUID> {

    List<StageTransitionEntity> findByOrderIdOrderById(UUID orderId);
}
