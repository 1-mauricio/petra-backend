package com.marmorarias.rls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marmorarias.crm.adapter.persistence.CustomerEntity;
import com.marmorarias.crm.application.CrmService;
import com.marmorarias.crm.domain.CustomerType;
import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.support.AbstractIntegrationTest;
import com.marmorarias.support.TestFixtures;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Isolamento RLS: usuário/role de uma organização não lê nem escreve linha de outra organização. */
class RlsIsolationTest extends AbstractIntegrationTest {

    @Autowired
    private CrmService crmService;
    @Autowired
    private DataSource dataSource;

    private TestFixtures fixtures;
    private UUID orgA;
    private UUID orgB;
    private TenantContext tenantA;
    private CustomerEntity customerB;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(dataSource);
        orgA = fixtures.criarOrganizacao("Marmoraria A");
        orgB = fixtures.criarOrganizacao("Marmoraria B");
        UUID userIdA = fixtures.criarUsuario(orgA, "admin");
        tenantA = new TenantContext(orgA, userIdA, Role.admin);

        customerB = crmService.criarCustomer(new TenantContext(orgB, fixtures.criarUsuario(orgB, "admin"), Role.admin),
                CustomerType.PJ, "Cliente da org B", "11222333000181", null, null);
        crmService.criarCustomer(tenantA, CustomerType.PJ, "Cliente da org A", "11222333000181", null, null);
    }

    @Test
    void listagemDaOrgANaoInclueClienteDaOrgB() {
        List<CustomerEntity> clientesDeA = crmService.listarCustomers(tenantA);
        assertEquals(1, clientesDeA.size());
        assertTrue(clientesDeA.stream().noneMatch(c -> c.getId().equals(customerB.getId())));
    }

    @Test
    void insertComOrganizationIdDivergenteDoContextoEBarradoPelaRls() {
        // tenta gravar uma linha de customer "da org B" enquanto o contexto RLS está setado para A —
        // o WITH CHECK da policy (V11) tem que rejeitar, mesmo com grant de INSERT em customer.
        fixtures.executarNaOrg(orgA, con -> {
            try (var ps = con.prepareStatement(
                    "INSERT INTO customer (id, organization_id, tipo, nome, cpf_cnpj) VALUES (?, ?, 'PJ', 'Invasor', '11222333000181')")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, orgB);
                assertThrows(SQLException.class, ps::executeUpdate);
            }
        });
    }
}
