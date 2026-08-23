package com.marmorarias.orders.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Append-only (grants revogam UPDATE/DELETE para app_user — V11): trilha de auditoria genérica. */
@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    private UUID actor;

    @Column(nullable = false)
    private String entidade;

    @Column(name = "entidade_id", nullable = false)
    private UUID entidadeId;

    @Column(nullable = false)
    private String acao;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(UUID organizationId, UUID actor, String entidade, UUID entidadeId, String acao) {
        this.organizationId = organizationId;
        this.actor = actor;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.acao = acao;
    }
}
