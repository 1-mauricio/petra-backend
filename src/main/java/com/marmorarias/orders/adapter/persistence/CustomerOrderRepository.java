package com.marmorarias.orders.adapter.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrderEntity, UUID> {

    long countByOrganizationIdAndCreatedAtGreaterThanEqual(UUID organizationId, OffsetDateTime desde);

    @Query("""
            select new com.marmorarias.orders.adapter.persistence.CustomerOrderListItem(
                o.id, o.customerId, c.nome, o.currentQuoteVersionId, o.state,
                o.valorTaxaCancelamento, o.motivoCancelamento, o.createdAt, o.updatedAt)
            from CustomerOrderEntity o, CustomerEntity c
            where o.customerId = c.id and o.organizationId = :organizationId
            order by o.createdAt desc
            """)
    List<CustomerOrderListItem> listarComCliente(@Param("organizationId") UUID organizationId);

    @Query("""
            select new com.marmorarias.orders.adapter.persistence.CustomerOrderListItem(
                o.id, o.customerId, c.nome, o.currentQuoteVersionId, o.state,
                o.valorTaxaCancelamento, o.motivoCancelamento, o.createdAt, o.updatedAt)
            from CustomerOrderEntity o, CustomerEntity c
            where o.customerId = c.id and o.id = :id
            """)
    Optional<CustomerOrderListItem> buscarComClientePorId(@Param("id") UUID id);

    @Query("""
            select new com.marmorarias.orders.adapter.persistence.CustomerOrderListItem(
                o.id, o.customerId, c.nome, o.currentQuoteVersionId, o.state,
                o.valorTaxaCancelamento, o.motivoCancelamento, o.createdAt, o.updatedAt)
            from CustomerOrderEntity o, CustomerEntity c
            where o.customerId = c.id and o.customerId = :customerId
            order by o.createdAt desc
            """)
    List<CustomerOrderListItem> listarPorCliente(@Param("customerId") UUID customerId);
}
