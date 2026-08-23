package com.marmorarias.quoting.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogItemRepository extends JpaRepository<CatalogItemEntity, UUID> {

    List<CatalogItemEntity> findByIdIn(List<UUID> ids);

    List<CatalogItemEntity> findByOrganizationId(UUID organizationId);
}
