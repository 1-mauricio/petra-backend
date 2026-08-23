package com.marmorarias.identity.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {

    List<UserProfileEntity> findByOrganizationId(UUID organizationId);

    long countByOrganizationId(UUID organizationId);
}
