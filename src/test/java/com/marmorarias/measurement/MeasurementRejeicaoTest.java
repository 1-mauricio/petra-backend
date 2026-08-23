package com.marmorarias.measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.measurement.adapter.persistence.MeasurementEntity;
import com.marmorarias.measurement.application.AprovarMedicaoRequest;
import com.marmorarias.measurement.application.MeasurementService;
import com.marmorarias.measurement.application.PecaMedidaRequest;
import com.marmorarias.measurement.domain.MeasurementStatus;
import com.marmorarias.orders.adapter.persistence.CustomerOrderEntity;
import com.marmorarias.orders.application.OrderService;
import com.marmorarias.orders.domain.OrderState;
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

class MeasurementRejeicaoTest extends AbstractIntegrationTest {

    @Autowired
    private QuoteService quoteService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private MeasurementService measurementService;
    @Autowired
    private DataSource dataSource;

    private TestFixtures fixtures;
    private TenantContext tenant;
    private CustomerOrderEntity order;
    private UUID materialId;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(dataSource);
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Rejeicao Test");
        UUID userId = fixtures.criarUsuario(orgId, "admin");
        tenant = new TenantContext(orgId, userId, Role.admin);
        UUID customerId = fixtures.criarCustomer(orgId);
        materialId = fixtures.criarMaterial(orgId, new BigDecimal("500.00"));

        CriarOrcamentoRequest request = new CriarOrcamentoRequest(customerId, null,
                List.of(new PecaRequest(materialId, new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of())),
                Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        QuoteVersionEntity versao = quoteService.criarOrcamento(tenant, request);
        quoteService.enviar(tenant, versao.getId());
        quoteService.aprovar(tenant, versao.getId());

        order = orderService.criarPedido(tenant, customerId, versao.getId());
        orderService.transicionar(tenant, order.getId(), OrderState.APROVACAO, "aprovado pelo cliente");
        orderService.transicionar(tenant, order.getId(), OrderState.PEDIDO, "virou pedido");
        orderService.transicionar(tenant, order.getId(), OrderState.LEVANTAMENTO_TECNICO, "medindo");
    }

    @Test
    void rejeitaMedicaoPendente() {
        MeasurementEntity medicao = measurementService.registrarMedicao(tenant, order.getId(), tenant.userId(),
                List.of(new PecaMedidaRequest(materialId, "peça 1", new BigDecimal("2"), new BigDecimal("1"), 1,
                        BigDecimal.ZERO)));

        MeasurementEntity rejeitada = measurementService.rejeitarMedicao(tenant, medicao.getId());

        assertEquals(MeasurementStatus.REJEITADO, rejeitada.getStatus());
    }

    @Test
    void naoDeixaRejeitarMedicaoJaAprovada() {
        MeasurementEntity medicao = measurementService.registrarMedicao(tenant, order.getId(), tenant.userId(),
                List.of(new PecaMedidaRequest(materialId, "peça 1", new BigDecimal("2"), new BigDecimal("1"), 1,
                        BigDecimal.ZERO)));
        measurementService.aprovarMedicao(tenant, medicao.getId(),
                new AprovarMedicaoRequest(Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null));

        assertThrows(IllegalStateException.class, () -> measurementService.rejeitarMedicao(tenant, medicao.getId()));
    }
}
