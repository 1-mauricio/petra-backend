package com.marmorarias.measurement.adapter.persistence;

import com.marmorarias.measurement.domain.MeasurementStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeasurementRepository extends JpaRepository<MeasurementEntity, UUID> {

    List<MeasurementEntity> findByOrderId(UUID orderId);

    List<MeasurementEntity> findByOrganizationIdOrderByDataMedicaoDesc(UUID organizationId);

    boolean existsByOrderIdAndStatus(UUID orderId, MeasurementStatus status);
}
