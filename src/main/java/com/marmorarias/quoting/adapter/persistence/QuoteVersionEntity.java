package com.marmorarias.quoting.adapter.persistence;

import com.marmorarias.quoting.domain.QuoteVersionStatus;
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
import java.time.Instant;
import java.util.UUID;

/** Versão imutável após APROVADO — reforçado por trigger no banco (V5); aqui só refletimos o estado. */
@Entity
@Table(name = "quote_version")
public class QuoteVersionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "quote_id", nullable = false)
    private UUID quoteId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private QuoteVersionStatus status;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected QuoteVersionEntity() {
    }

    public QuoteVersionEntity(UUID organizationId, UUID quoteId, int versionNumber, BigDecimal valorTotal) {
        this.organizationId = organizationId;
        this.quoteId = quoteId;
        this.versionNumber = versionNumber;
        this.valorTotal = valorTotal;
        this.status = QuoteVersionStatus.RASCUNHO;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getQuoteId() {
        return quoteId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public QuoteVersionStatus getStatus() {
        return status;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void rejeitar() {
        if (status != QuoteVersionStatus.ENVIADO) {
            throw new IllegalStateException("quote_version " + id + " precisa estar ENVIADO para ser rejeitada");
        }
        this.status = QuoteVersionStatus.REJEITADO;
    }

    public void atualizarValorTotal(BigDecimal valorTotal) {
        exigirStatus(QuoteVersionStatus.RASCUNHO, "editada");
        this.valorTotal = valorTotal;
    }

    public void enviar() {
        exigirStatus(QuoteVersionStatus.RASCUNHO, "enviada");
        this.status = QuoteVersionStatus.ENVIADO;
    }

    public void aprovar() {
        if (status != QuoteVersionStatus.ENVIADO) {
            throw new IllegalStateException("quote_version " + id + " precisa estar ENVIADO para ser aprovada");
        }
        this.status = QuoteVersionStatus.APROVADO;
        this.approvedAt = Instant.now();
    }

    private void exigirStatus(QuoteVersionStatus esperado, String acao) {
        if (status != esperado) {
            throw new IllegalStateException("quote_version " + id + " não pode ser " + acao + " a partir de " + status);
        }
    }
}
