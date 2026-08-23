package com.marmorarias.platformbilling;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marmorarias.identity.application.UserService;
import com.marmorarias.identity.domain.LimiteUsuariosExcedidoException;
import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.orders.application.OrderService;
import com.marmorarias.orders.domain.LimitePedidosExcedidoException;
import com.marmorarias.platformbilling.domain.PlanLimits;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionEntity;
import com.marmorarias.quoting.application.CriarOrcamentoRequest;
import com.marmorarias.quoting.application.PecaRequest;
import com.marmorarias.quoting.application.QuoteService;
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

/**
 * Prova os guards de limite do plano basico (sem BILLING_PLANO_OVERRIDE, org sem subscription
 * cai no plano mais restrito) contra Postgres real via Testcontainers.
 */
class PlanLimitsGuardTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private QuoteService quoteService;
    @Autowired
    private DataSource dataSource;

    private TestFixtures fixtures;
    private TenantContext tenant;
    private UUID customerId;
    private UUID quoteVersionId;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(dataSource);
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Limite Test");
        UUID userId = fixtures.criarUsuario(orgId, "admin");
        tenant = new TenantContext(orgId, userId, Role.admin);
        customerId = fixtures.criarCustomer(orgId);
        UUID materialId = fixtures.criarMaterial(orgId, new BigDecimal("500.00"));

        CriarOrcamentoRequest request = new CriarOrcamentoRequest(customerId, null,
                List.of(new PecaRequest(materialId, new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of())),
                Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        QuoteVersionEntity versao = quoteService.criarOrcamento(tenant, request);
        quoteVersionId = versao.getId();
    }

    @Test
    void bloqueiaNovoPedidoAoAtingirLimiteDoPlanoBasico() {
        for (int i = 0; i < PlanLimits.BASICO_MAX_PEDIDOS_POR_MES; i++) {
            fixtures.executarNaOrg(tenant.organizationId(), con -> {
                try (var ps = con.prepareStatement(
                        "INSERT INTO customer_order (organization_id, customer_id, current_quote_version_id) "
                                + "VALUES (?, ?, ?)")) {
                    ps.setObject(1, tenant.organizationId());
                    ps.setObject(2, customerId);
                    ps.setObject(3, quoteVersionId);
                    ps.executeUpdate();
                }
            });
        }

        LimitePedidosExcedidoException ex = assertThrows(LimitePedidosExcedidoException.class,
                () -> orderService.criarPedido(tenant, customerId, quoteVersionId));
        org.junit.jupiter.api.Assertions.assertEquals(true, ex.getMessage().contains("basico"));
    }

    @Test
    void bloqueiaConviteAoAtingirLimiteDeUsuariosDoPlanoBasico() {
        fixtures.criarUsuario(tenant.organizationId(), "comercial");
        fixtures.criarUsuario(tenant.organizationId(), "producao");

        assertThrows(LimiteUsuariosExcedidoException.class,
                () -> userService.convidar(tenant, "novo@teste.com", "Novo", Role.comercial));
    }
}
