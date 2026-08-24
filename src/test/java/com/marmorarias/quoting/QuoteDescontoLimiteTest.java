package com.marmorarias.quoting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.quoting.application.CriarOrcamentoRequest;
import com.marmorarias.quoting.application.PecaRequest;
import com.marmorarias.quoting.application.QuoteService;
import com.marmorarias.quoting.domain.DescontoExigeAdminException;
import com.marmorarias.support.AbstractIntegrationTest;
import com.marmorarias.support.TestFixtures;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Desconto acima do limite configurado (org_settings.desconto_limite_perc) exige role admin. */
class QuoteDescontoLimiteTest extends AbstractIntegrationTest {

    @Autowired
    private QuoteService quoteService;
    @Autowired
    private DataSource dataSource;

    private TestFixtures fixtures;
    private UUID orgId;
    private UUID customerId;
    private UUID materialId;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(dataSource);
        orgId = fixtures.criarOrganizacao("Marmoraria Desconto Limite Test");
        customerId = fixtures.criarCustomer(orgId);
        materialId = fixtures.criarMaterial(orgId, new BigDecimal("500.00"));
        fixtures.ajustarLimiteDesconto(orgId, new BigDecimal("10.00"));
    }

    private CriarOrcamentoRequest requestComDesconto(BigDecimal desconto) {
        return new CriarOrcamentoRequest(customerId, null,
                List.of(new PecaRequest(materialId, new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of())),
                Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, desconto, null, null);
    }

    @Test
    void comercialNaoPodeConcederDescontoAcimaDoLimite() {
        UUID userId = fixtures.criarUsuario(orgId, "comercial");
        TenantContext comercial = new TenantContext(orgId, userId, Role.comercial);

        assertThrows(DescontoExigeAdminException.class,
                () -> quoteService.criarOrcamento(comercial, requestComDesconto(new BigDecimal("0.15"))));
    }

    @Test
    void comercialPodeConcederDescontoDentroDoLimite() {
        UUID userId = fixtures.criarUsuario(orgId, "comercial");
        TenantContext comercial = new TenantContext(orgId, userId, Role.comercial);

        assertDoesNotThrow(() -> quoteService.criarOrcamento(comercial, requestComDesconto(new BigDecimal("0.05"))));
    }

    @Test
    void adminPodeConcederDescontoAcimaDoLimite() {
        UUID userId = fixtures.criarUsuario(orgId, "admin");
        TenantContext admin = new TenantContext(orgId, userId, Role.admin);

        assertDoesNotThrow(() -> quoteService.criarOrcamento(admin, requestComDesconto(new BigDecimal("0.50"))));
    }
}
