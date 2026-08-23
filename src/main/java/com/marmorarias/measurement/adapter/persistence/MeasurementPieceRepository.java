package com.marmorarias.measurement.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeasurementPieceRepository extends JpaRepository<MeasurementPieceEntity, UUID> {

    List<MeasurementPieceEntity> findByMeasurementId(UUID measurementId);
}
