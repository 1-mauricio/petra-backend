package com.marmorarias.crm.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<LeadEntity, UUID> {

    List<LeadEntity> findByOrganizationId(UUID organizationId);

    @Query("""
            select new com.marmorarias.crm.adapter.persistence.LeadListItem(
                l.id, l.customerId, c.nome, l.origem, l.status, l.motivoPerda, l.createdAt)
            from LeadEntity l left join CustomerEntity c on l.customerId = c.id
            where l.organizationId = :organizationId
            order by l.createdAt desc
            """)
    List<LeadListItem> listarComCliente(@Param("organizationId") UUID organizationId);
}
