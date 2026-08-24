package com.marmorarias.identity.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Tolerâncias de divergência medição x orçamento e limite de desconto, por organização (invariantes 1 e 2). */
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

    /** Desconto acima deste percentual do preço ao cliente exige role admin (ver QuoteService). */
    @Column(name = "desconto_limite_perc", nullable = false)
    private BigDecimal descontoLimitePerc;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fator_perda", nullable = false)
    private Map<String, BigDecimal> fatorPerda = new HashMap<>();

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

    public BigDecimal getDescontoLimitePerc() {
        return descontoLimitePerc;
    }

    public Map<String, BigDecimal> getFatorPerda() {
        return fatorPerda;
    }

    public void atualizar(BigDecimal toleranciaPerc, BigDecimal toleranciaAbs, BigDecimal descontoLimitePerc,
                           Map<String, BigDecimal> fatorPerda) {
        if (toleranciaPerc != null) {
            this.toleranciaPerc = toleranciaPerc;
        }
        if (toleranciaAbs != null) {
            this.toleranciaAbs = toleranciaAbs;
        }
        if (descontoLimitePerc != null) {
            this.descontoLimitePerc = descontoLimitePerc;
        }
        if (fatorPerda != null) {
            this.fatorPerda = fatorPerda;
        }
    }
}
