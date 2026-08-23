package com.marmorarias.identity.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Tolerâncias de divergência medição x orçamento, configuráveis por organização (invariante 2). */
@Entity
@Table(name = "org_settings")
public class OrgSettingsEntity {

    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "tolerancia_perc", nullable = false)
    private BigDecimal toleranciaPerc;

    @Column(name = "tolerancia_abs", nullable = false)
    private BigDecimal toleranciaAbs;

    protected OrgSettingsEntity() {
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public BigDecimal getToleranciaPerc() {
        return toleranciaPerc;
    }

    public BigDecimal getToleranciaAbs() {
        return toleranciaAbs;
    }
}
