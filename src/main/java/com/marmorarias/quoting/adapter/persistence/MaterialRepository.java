package com.marmorarias.quoting.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<MaterialEntity, UUID> {

    List<MaterialEntity> findByIdIn(List<UUID> ids);

    List<MaterialEntity> findByOrganizationId(UUID organizationId);
}
