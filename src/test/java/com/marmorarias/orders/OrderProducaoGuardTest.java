package com.marmorarias.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.orders.adapter.persistence.CustomerOrderEntity;
import com.marmorarias.orders.application.OrderService;
import com.marmorarias.orders.domain.OrderState;
import com.marmorarias.orders.domain.TransicaoInvalidaException;
import com.marmorarias.quoting.application.CriarOrcamentoRequest;
import com.marmorarias.quoting.application.PecaRequest;
import com.marmorarias.quoting.application.QuoteService;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionEntity;
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

/** Invariante 1: PRODUCAO só é alcançável com measurement APROVADO e sem revisão de orçamento pendente. */
class OrderProducaoGuardTest extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;
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
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Guard Test");
        UUID userId = fixtures.criarUsuario(orgId, "admin");
        tenant = new TenantContext(orgId, userId, Role.admin);
        customerId = fixtures.criarCustomer(orgId);
        materialId = fixtures.criarMaterial(orgId, new BigDecimal("500.00"));
    }

    private CustomerOrderEntity pedidoAteLevantamentoTecnico() {
        CriarOrcamentoRequest request = new CriarOrcamentoRequest(customerId, null,
                List.of(new PecaRequest(materialId, new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of())),
                Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        QuoteVersionEntity versao = quoteService.criarOrcamento(tenant, request);
        quoteService.enviar(tenant, versao.getId());
        quoteService.aprovar(tenant, versao.getId());

        CustomerOrderEntity order = orderService.criarPedido(tenant, customerId, versao.getId());
        orderService.transicionar(tenant, order.getId(), OrderState.APROVACAO, "aprovado pelo cliente");
        orderService.transicionar(tenant, order.getId(), OrderState.PEDIDO, "virou pedido");
        orderService.transicionar(tenant, order.getId(), OrderState.LEVANTAMENTO_TECNICO, "medindo");
        return order;
    }

    @Test
    void bloqueiaProducaoNoAppSemMedicaoAprovada() {
        CustomerOrderEntity order = pedidoAteLevantamentoTecnico();

        TransicaoInvalidaException ex = assertThrows(TransicaoInvalidaException.class,
                () -> orderService.transicionar(tenant, order.getId(), OrderState.PRODUCAO, "pulando a medição"));
        assertEquals(true, ex.getMessage().contains("medição"));
    }

    @Test
    void bloqueiaProducaoNoBancoMesmoBurlandoOGuardDeAplicacao() {
        CustomerOrderEntity order = pedidoAteLevantamentoTecnico();

        // Simula um bug na camada de aplicação: tenta escrever o estado direto, sem passar pelo
        // guard do OrderService. A trigger fn_customer_order_guard_producao (V6) tem que barrar.
        fixtures.executarNaOrg(tenant.organizationId(), con -> {
            try (var ps = con.prepareStatement("UPDATE customer_order SET state = 'PRODUCAO' WHERE id = ?")) {
                ps.setObject(1, order.getId());
                assertThrows(SQLException.class, ps::executeUpdate);
            }
        });
    }
}
