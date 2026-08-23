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

/**
 * Preço de catálogo é snapshotado aqui no momento da criação (invariante 4): preco_unitario_snapshot
 * nunca é recalculado a partir de material/catalog_item depois de gravado, mesmo se o catálogo mudar.
 */
@Entity
@Table(name = "quote_line_item")
public class QuoteLineItemEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "quote_version_id", nullable = false)
    private UUID quoteVersionId;

    @Column(name = "material_id")
    private UUID materialId;

    @Column(name = "catalog_item_id")
    private UUID catalogItemId;

    private String descricao;

    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UnidadeMedida unidade;

    @Column(name = "preco_unitario_snapshot", nullable = false)
    private BigDecimal precoUnitarioSnapshot;

    @Column(nullable = false)
    private BigDecimal subtotal;

    protected QuoteLineItemEntity() {
    }

    public QuoteLineItemEntity(UUID organizationId, UUID quoteVersionId, UUID materialId, UUID catalogItemId,
                                String descricao, BigDecimal quantidade, UnidadeMedida unidade,
                                BigDecimal precoUnitarioSnapshot, BigDecimal subtotal) {
        this.organizationId = organizationId;
        this.quoteVersionId = quoteVersionId;
        this.materialId = materialId;
        this.catalogItemId = catalogItemId;
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.unidade = unidade;
        this.precoUnitarioSnapshot = precoUnitarioSnapshot;
        this.subtotal = subtotal;
    }

    public UUID getId() {
        return id;
    }

    public UUID getQuoteVersionId() {
        return quoteVersionId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getCatalogItemId() {
        return catalogItemId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public UnidadeMedida getUnidade() {
        return unidade;
    }

    public BigDecimal getPrecoUnitarioSnapshot() {
        return precoUnitarioSnapshot;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
