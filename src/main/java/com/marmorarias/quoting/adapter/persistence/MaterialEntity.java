package com.marmorarias.quoting.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "material")
public class MaterialEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    private String tipo;

    private String cor;

    @Column(name = "preco_m2", nullable = false)
    private BigDecimal precoM2;

    @Column(name = "largura_chapa", nullable = false)
    private BigDecimal larguraChapa;

    @Column(name = "comprimento_chapa", nullable = false)
    private BigDecimal comprimentoChapa;

    @Column(nullable = false)
    private boolean ativo;

    protected MaterialEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCor() {
        return cor;
    }

    public BigDecimal getPrecoM2() {
        return precoM2;
    }

    public BigDecimal getLarguraChapa() {
        return larguraChapa;
    }

    public BigDecimal getComprimentoChapa() {
        return comprimentoChapa;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
