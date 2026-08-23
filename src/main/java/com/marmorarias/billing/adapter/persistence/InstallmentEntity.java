package com.marmorarias.billing.adapter.persistence;

import com.marmorarias.billing.domain.InstallmentStatus;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "installment")
public class InstallmentEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "receivable_id", nullable = false)
    private UUID receivableId;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private InstallmentStatus status;

    protected InstallmentEntity() {
    }

    public InstallmentEntity(UUID organizationId, UUID receivableId, int numero, BigDecimal valor,
                              LocalDate vencimento) {
        this.organizationId = organizationId;
        this.receivableId = receivableId;
        this.numero = numero;
        this.valor = valor;
        this.vencimento = vencimento;
        this.status = InstallmentStatus.PENDENTE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReceivableId() {
        return receivableId;
    }

    public int getNumero() {
        return numero;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public InstallmentStatus getStatus() {
        return status;
    }

    public void marcarPaga() {
        this.status = InstallmentStatus.PAGO;
    }
}
