package com.marmorarias.identity.adapter.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgSettingsRepository extends JpaRepository<OrgSettingsEntity, UUID> {
}
