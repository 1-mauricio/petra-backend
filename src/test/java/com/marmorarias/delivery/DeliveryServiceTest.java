package com.marmorarias.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.marmorarias.delivery.adapter.persistence.DeliveryEntity;
import com.marmorarias.delivery.application.DeliveryService;
import com.marmorarias.delivery.domain.DeliveryStatus;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** DeliveryService: guard de agendamento (produção precisa estar concluída) e transições de status. */
class DeliveryServiceTest extends AbstractIntegrationTest {

    @Autowired
    private DeliveryService deliveryService;
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
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Entrega Test");
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
    void agendarSemNenhumaTarefaDeProducaoFalha() {
        assertThrows(IllegalStateException.class, () -> deliveryService.agendar(tenant, orderId, Instant.now()));
    }

    @Test
    void agendarComTarefaPendenteFalha() {
        productionService.criar(tenant, orderId, "Cortar chapa", tenant.userId());

        assertThrows(IllegalStateException.class, () -> deliveryService.agendar(tenant, orderId, Instant.now()));
    }

    @Test
    void agendarComTodasAsTarefasConcluidasFunciona() {
        ProductionTaskEntity task = productionService.criar(tenant, orderId, "Cortar chapa", tenant.userId());
        productionService.atualizarStatus(tenant, task.getId(), ProductionTaskStatus.CONCLUIDA);

        DeliveryEntity delivery = deliveryService.agendar(tenant, orderId, Instant.now());

        assertEquals(DeliveryStatus.AGENDADA, delivery.getStatus());
    }

    @Test
    void atualizarStatusParaEntregueRegistraDataDeEntrega() {
        ProductionTaskEntity task = productionService.criar(tenant, orderId, "Cortar chapa", tenant.userId());
        productionService.atualizarStatus(tenant, task.getId(), ProductionTaskStatus.CONCLUIDA);
        DeliveryEntity delivery = deliveryService.agendar(tenant, orderId, Instant.now());

        DeliveryEntity entregue = deliveryService.atualizarStatus(tenant, delivery.getId(), DeliveryStatus.ENTREGUE);

        assertEquals(DeliveryStatus.ENTREGUE, entregue.getStatus());
        assertNotNull(entregue.getDataEntrega());
    }

    @Test
    void atualizarStatusDeEntregaInexistenteFalha() {
        assertThrows(NoSuchElementException.class,
                () -> deliveryService.atualizarStatus(tenant, UUID.randomUUID(), DeliveryStatus.EM_ROTA));
    }
}
