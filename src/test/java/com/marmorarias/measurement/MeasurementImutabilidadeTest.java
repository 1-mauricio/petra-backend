package com.marmorarias.measurement;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.measurement.adapter.persistence.MeasurementEntity;
import com.marmorarias.measurement.application.AprovarMedicaoRequest;
import com.marmorarias.measurement.application.MeasurementService;
import com.marmorarias.measurement.application.PecaMedidaRequest;
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
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Invariante 5: measurement é imutável após APROVADO — nova medição é um novo registro. */
class MeasurementImutabilidadeTest extends AbstractIntegrationTest {

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
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Imutabilidade Test");
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

    private MeasurementEntity medicaoAprovada() {
        MeasurementEntity medicao = measurementService.registrarMedicao(tenant, order.getId(), tenant.userId(),
                List.of(new PecaMedidaRequest(materialId, "peça 1", new BigDecimal("2"), new BigDecimal("1"), 1,
                        BigDecimal.ZERO)));
        measurementService.aprovarMedicao(tenant, medicao.getId(),
                new AprovarMedicaoRequest(Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null));
        return medicao;
    }

    @Test
    void naoDeixaAprovarMedicaoJaAprovada() {
        MeasurementEntity medicao = medicaoAprovada();

        assertThrows(IllegalStateException.class, () -> measurementService.aprovarMedicao(tenant, medicao.getId(),
                new AprovarMedicaoRequest(Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null)));
    }

    @Test
    void bloqueiaUpdateNoBancoMesmoBurlandoOGuardDeAplicacao() {
        MeasurementEntity medicao = medicaoAprovada();

        // Simula um bug na camada de aplicação: escreve direto no banco, sem passar pelo guard de
        // MeasurementEntity.aprovar()/rejeitar(). A trigger fn_measurement_bloquear_update_aprovado
        // (V7) tem que barrar.
        fixtures.executarNaOrg(tenant.organizationId(), con -> {
            try (var ps = con.prepareStatement("UPDATE measurement SET status = 'REJEITADO' WHERE id = ?")) {
                ps.setObject(1, medicao.getId());
                assertThrows(SQLException.class, ps::executeUpdate);
            }
        });
    }
}
