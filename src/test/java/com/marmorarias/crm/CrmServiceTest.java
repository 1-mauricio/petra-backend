package com.marmorarias.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marmorarias.crm.adapter.persistence.CustomerEntity;
import com.marmorarias.crm.adapter.persistence.LeadEntity;
import com.marmorarias.crm.application.CrmService;
import com.marmorarias.crm.domain.CustomerType;
import com.marmorarias.crm.domain.LeadStatus;
import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.support.AbstractIntegrationTest;
import com.marmorarias.support.TestFixtures;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** CrmService: transições de lead e isolamento por org. */
class CrmServiceTest extends AbstractIntegrationTest {

    @Autowired
    private CrmService crmService;
    @Autowired
    private DataSource dataSource;

    private TestFixtures fixtures;
    private TenantContext tenant;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(dataSource);
        UUID orgId = fixtures.criarOrganizacao("Marmoraria CRM Test");
        UUID userId = fixtures.criarUsuario(orgId, "admin");
        tenant = new TenantContext(orgId, userId, Role.admin);
    }

    private UUID criarLead() {
        CustomerEntity customer = crmService.criarCustomer(tenant, CustomerType.PF, "Cliente Lead",
                "12345678909", "cliente@teste.com", "11999999999");
        return crmService.criarLead(tenant, customer.getId(), "site").getId();
    }

    @Test
    void leadNasceNovo() {
        UUID leadId = criarLead();
        // moverStatus para o próprio NOVO não muda nada, mas prova que o lead existe com esse status
        LeadEntity lead = crmService.moverStatus(tenant, leadId, LeadStatus.NOVO, null);
        assertEquals(LeadStatus.NOVO, lead.getStatus());
    }

    @Test
    void moverStatusTransicionaLivrementeEntreEstadosNaoTerminais() {
        UUID leadId = criarLead();

        LeadEntity contatado = crmService.moverStatus(tenant, leadId, LeadStatus.CONTATADO, null);
        assertEquals(LeadStatus.CONTATADO, contatado.getStatus());

        LeadEntity qualificado = crmService.moverStatus(tenant, leadId, LeadStatus.QUALIFICADO, null);
        assertEquals(LeadStatus.QUALIFICADO, qualificado.getStatus());
    }

    @Test
    void marcarPerdidoExigeMotivo() {
        UUID leadId = criarLead();

        assertThrows(IllegalArgumentException.class,
                () -> crmService.moverStatus(tenant, leadId, LeadStatus.PERDIDO, null));
        assertThrows(IllegalArgumentException.class,
                () -> crmService.moverStatus(tenant, leadId, LeadStatus.PERDIDO, "  "));
    }

    @Test
    void marcarPerdidoComMotivoRegistraStatusEMotivo() {
        UUID leadId = criarLead();

        LeadEntity lead = crmService.moverStatus(tenant, leadId, LeadStatus.PERDIDO, "cliente desistiu");

        assertEquals(LeadStatus.PERDIDO, lead.getStatus());
        assertEquals("cliente desistiu", lead.getMotivoPerda());
    }

    @Test
    void marcarLeadConvertidoAtualizaStatus() {
        UUID leadId = criarLead();

        LeadEntity lead = crmService.marcarLeadConvertido(tenant, leadId);

        assertEquals(LeadStatus.CONVERTIDO, lead.getStatus());
    }

    @Test
    void buscarCustomerInexistenteFalha() {
        assertThrows(NoSuchElementException.class, () -> crmService.buscarCustomer(tenant, UUID.randomUUID()));
    }

    @Test
    void customerNaoVazaEntreOrganizacoes() {
        CustomerEntity customer = crmService.criarCustomer(tenant, CustomerType.PJ, "Empresa Teste",
                "11222333000181", "empresa@teste.com", "1133334444");

        UUID outraOrgId = fixtures.criarOrganizacao("Outra Marmoraria");
        UUID outroUserId = fixtures.criarUsuario(outraOrgId, "admin");
        TenantContext outroTenant = new TenantContext(outraOrgId, outroUserId, Role.admin);

        assertThrows(NoSuchElementException.class, () -> crmService.buscarCustomer(outroTenant, customer.getId()));
    }
}
