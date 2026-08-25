package com.marmorarias.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.orders.adapter.persistence.CustomerOrderEntity;
import com.marmorarias.orders.application.OrderService;
import com.marmorarias.orders.domain.OrderState;
import com.marmorarias.production.adapter.persistence.ProductionTaskEntity;
import com.marmorarias.production.application.ProductionService;
import com.marmorarias.production.domain.ProductionTaskStatus;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionEntity;
import com.marmorarias.quoting.application.CriarOrcamentoRequest;
import com.marmorarias.quoting.application.PecaRequest;
import com.marmorarias.quoting.application.QuoteService;
import com.marmorarias.support.AbstractIntegrationTest;
import com.marmorarias.support.TestFixtures;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** ProductionService: transições de status de tarefa e isolamento por org. */
class ProductionServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ProductionService productionService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private QuoteService quoteService;
    @Autowired
    private DataSource dataSource;

    private TestFixtures fixtures;
    private TenantContext tenant;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(dataSource);
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Produção Test");
        UUID userId = fixtures.criarUsuario(orgId, "admin");
        tenant = new TenantContext(orgId, userId, Role.admin);
        UUID customerId = fixtures.criarCustomer(orgId);
        UUID materialId = fixtures.criarMaterial(orgId, new BigDecimal("500.00"));

        CriarOrcamentoRequest request = new CriarOrcamentoRequest(customerId, null,
                List.of(new PecaRequest(materialId, new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of())),
                Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        QuoteVersionEntity versao = quoteService.criarOrcamento(tenant, request);
        quoteService.enviar(tenant, versao.getId());
        quoteService.aprovar(tenant, versao.getId());

        CustomerOrderEntity order = orderService.criarPedido(tenant, customerId, versao.getId());
        orderService.transicionar(tenant, order.getId(), OrderState.APROVACAO, "aprovado pelo cliente");
        orderService.transicionar(tenant, order.getId(), OrderState.PEDIDO, "virou pedido");
        orderId = order.getId();
    }

    @Test
    void tarefaNasceComoPendente() {
        ProductionTaskEntity task = productionService.criar(tenant, orderId, "Cortar chapa", tenant.userId());

        assertEquals(ProductionTaskStatus.PENDENTE, task.getStatus());
    }

    @Test
    void atualizarStatusTransicionaAteConcluida() {
        ProductionTaskEntity task = productionService.criar(tenant, orderId, "Polir bancada", tenant.userId());

        productionService.atualizarStatus(tenant, task.getId(), ProductionTaskStatus.EM_ANDAMENTO);
        ProductionTaskEntity concluida = productionService.atualizarStatus(tenant, task.getId(),
                ProductionTaskStatus.CONCLUIDA);

        assertEquals(ProductionTaskStatus.CONCLUIDA, concluida.getStatus());
    }

    @Test
    void atualizarStatusDeTarefaInexistenteFalha() {
        assertThrows(NoSuchElementException.class,
                () -> productionService.atualizarStatus(tenant, UUID.randomUUID(), ProductionTaskStatus.CONCLUIDA));
    }

    @Test
    void tarefaNaoVazaEntreOrganizacoes() {
        ProductionTaskEntity task = productionService.criar(tenant, orderId, "Instalar", tenant.userId());

        UUID outraOrgId = fixtures.criarOrganizacao("Outra Marmoraria");
        UUID outroUserId = fixtures.criarUsuario(outraOrgId, "admin");
        TenantContext outroTenant = new TenantContext(outraOrgId, outroUserId, Role.admin);

        assertThrows(NoSuchElementException.class,
                () -> productionService.atualizarStatus(outroTenant, task.getId(), ProductionTaskStatus.CONCLUIDA));
    }
}
