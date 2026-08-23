package com.marmorarias.quoting;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionEntity;
import com.marmorarias.quoting.application.CriarOrcamentoRequest;
import com.marmorarias.quoting.application.PecaRequest;
import com.marmorarias.quoting.application.QuoteService;
import com.marmorarias.support.AbstractIntegrationTest;
import com.marmorarias.support.TestFixtures;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Invariante 3: orçamento aprovado nunca é sobrescrito — vira read-only no banco. */
class QuoteVersionImutabilidadeTest extends AbstractIntegrationTest {

    @Autowired
    private QuoteService quoteService;
    @Autowired
    private DataSource dataSource;

    private TestFixtures fixtures;
    private TenantContext tenant;
    private UUID customerId;
    private UUID materialId;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(dataSource);
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Quote Imutavel Test");
        UUID userId = fixtures.criarUsuario(orgId, "admin");
        tenant = new TenantContext(orgId, userId, Role.admin);
        customerId = fixtures.criarCustomer(orgId);
        materialId = fixtures.criarMaterial(orgId, new BigDecimal("500.00"));
    }

    @Test
    void bloqueiaUpdateNoBancoEmVersaoAprovadaMesmoBurlandoAAplicacao() {
        CriarOrcamentoRequest request = new CriarOrcamentoRequest(customerId, null,
                List.of(new PecaRequest(materialId, new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of())),
                Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        QuoteVersionEntity versao = quoteService.criarOrcamento(tenant, request);
        quoteService.enviar(tenant, versao.getId());
        quoteService.aprovar(tenant, versao.getId());

        // Simula um bug na camada de aplicação: escreve direto no banco, sem passar por
        // QuoteService.revisar (que cria uma nova versão em vez de editar a aprovada). A trigger
        // fn_quote_version_bloquear_update_aprovado (V5) tem que barrar.
        fixtures.executarNaOrg(tenant.organizationId(), con -> {
            try (var ps = con.prepareStatement("UPDATE quote_version SET valor_total = 999 WHERE id = ?")) {
                ps.setObject(1, versao.getId());
                assertThrows(SQLException.class, ps::executeUpdate);
            }
        });
    }
}
