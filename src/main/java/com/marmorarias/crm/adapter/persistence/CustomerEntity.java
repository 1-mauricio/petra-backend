package com.marmorarias.crm.adapter.persistence;

import com.marmorarias.crm.domain.CustomerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "customer")
public class CustomerEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private CustomerType tipo;

    @Column(nullable = false)
    private String nome;

    @Column(name = "cpf_cnpj", nullable = false)
    private String cpfCnpj;

    private String email;

    private String telefone;

    protected CustomerEntity() {
    }

    public CustomerEntity(UUID organizationId, CustomerType tipo, String nome, String cpfCnpj, String email,
                           String telefone) {
        this.organizationId = organizationId;
        this.tipo = tipo;
        this.nome = nome;
        this.cpfCnpj = cpfCnpj;
        this.email = email;
        this.telefone = telefone;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public CustomerType getTipo() {
        return tipo;
    }

    public String getNome() {
        return nome;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }
}
