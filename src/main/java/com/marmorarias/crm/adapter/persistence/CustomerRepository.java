package com.marmorarias.crm.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    List<CustomerEntity> findByOrganizationId(UUID organizationId);
}
