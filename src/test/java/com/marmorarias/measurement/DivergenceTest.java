package com.marmorarias.measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.measurement.adapter.persistence.MeasurementEntity;
import com.marmorarias.measurement.application.AprovarMedicaoRequest;
import com.marmorarias.measurement.application.MeasurementApprovalResult;
import com.marmorarias.measurement.application.MeasurementService;
import com.marmorarias.measurement.application.PecaMedidaRequest;
import com.marmorarias.orders.adapter.persistence.CustomerOrderEntity;
import com.marmorarias.orders.application.OrderService;
import com.marmorarias.orders.domain.OrderState;
import com.marmorarias.orders.domain.TransicaoInvalidaException;
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
 * Invariante 2: divergência medição x orçamento aprovado acima da tolerância (5% ou R$500, o que
 * ocorrer primeiro) força REVISAO_ORCAMENTO e exige re-aprovação antes de liberar PRODUCAO.
 */
class DivergenceTest extends AbstractIntegrationTest {

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
    private UUID materialId;
    private CustomerOrderEntity order;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(dataSource);
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Divergência Test");
        UUID userId = fixtures.criarUsuario(orgId, "admin");
        tenant = new TenantContext(orgId, userId, Role.admin);
        UUID customerId = fixtures.criarCustomer(orgId);
        materialId = fixtures.criarMaterial(orgId, new BigDecimal("500.00"));

        // orçamento aprovado: 1 peça de 2m x 1m a R$500/m² = R$1000
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

    private AprovarMedicaoRequest paramsSemExtras() {
        return new AprovarMedicaoRequest(Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }

    @Test
    void divergenciaDentroDaToleranciaVaiDireitoParaProducao() {
        // medida real: 2m x 1.01m (~1% de diferença) — dentro da tolerância default (5% / R$500)
        MeasurementEntity medicao = measurementService.registrarMedicao(tenant, order.getId(), tenant.userId(),
                List.of(new PecaMedidaRequest(materialId, "peça 1", new BigDecimal("2"), new BigDecimal("1.01"), 1,
                        BigDecimal.ZERO)));

        MeasurementApprovalResult resultado = measurementService.aprovarMedicao(tenant, medicao.getId(), paramsSemExtras());

        assertFalse(resultado.divergencia().excedeTolerancia());

        orderService.transicionar(tenant, order.getId(), OrderState.PRODUCAO, "medição ok, sem divergência relevante");
        assertEquals("PRODUCAO", fixtures.lerEstadoPedido(tenant.organizationId(), order.getId()));
    }

    @Test
    void divergenciaAcimaDaToleranciaForcaRevisaoEBloqueiaProducaoAteReaprovacao() {
        // medida real: 2m x 3m (área triplicada) — dispara divergência bem acima de 5%/R$500
        MeasurementEntity medicao = measurementService.registrarMedicao(tenant, order.getId(), tenant.userId(),
                List.of(new PecaMedidaRequest(materialId, "peça 1", new BigDecimal("2"), new BigDecimal("3"), 1,
                        BigDecimal.ZERO)));

        AprovarMedicaoRequest params = paramsSemExtras();
        MeasurementApprovalResult resultado = measurementService.aprovarMedicao(tenant, medicao.getId(), params);
        assertTrue(resultado.divergencia().excedeTolerancia());

        CriarOrcamentoRequest novoOrcamento = new CriarOrcamentoRequest(order.getCustomerId(), null,
                List.of(new PecaRequest(materialId, new BigDecimal("2"), new BigDecimal("3"), List.of(), List.of())),
                Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        CustomerOrderEntity emRevisao = orderService.forcarRevisaoOrcamento(tenant, order.getId(), novoOrcamento,
                "divergência acima da tolerância");
        assertEquals(OrderState.REVISAO_ORCAMENTO, emRevisao.getState());
        assertEquals("REVISAO_ORCAMENTO", fixtures.lerEstadoPedido(tenant.organizationId(), order.getId()));

        // sem a nova versão reaprovada, PRODUCAO continua bloqueado mesmo com medição aprovada
        assertThrows(TransicaoInvalidaException.class,
                () -> orderService.transicionar(tenant, order.getId(), OrderState.PRODUCAO, "tentando pular a reaprovação"));

        quoteService.enviar(tenant, emRevisao.getCurrentQuoteVersionId());
        quoteService.aprovar(tenant, emRevisao.getCurrentQuoteVersionId());

        orderService.transicionar(tenant, order.getId(), OrderState.PRODUCAO, "orçamento revisado reaprovado");
        assertEquals("PRODUCAO", fixtures.lerEstadoPedido(tenant.organizationId(), order.getId()));
    }
}
