package com.marmorarias.quoting.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "catalog_item")
public class CatalogItemEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CatalogItemTipo tipo;

    private String descricao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UnidadeMedida unidade;

    @Column(nullable = false)
    private BigDecimal preco;

    @Column(nullable = false)
    private boolean ativo;

    protected CatalogItemEntity() {
    }

    public CatalogItemEntity(UUID organizationId, CatalogItemTipo tipo, String descricao, UnidadeMedida unidade,
                              BigDecimal preco) {
        this.organizationId = organizationId;
        this.tipo = tipo;
        this.descricao = descricao;
        this.unidade = unidade;
        this.preco = preco;
        this.ativo = true;
    }

    public void atualizarPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public CatalogItemTipo getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public UnidadeMedida getUnidade() {
        return unidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
