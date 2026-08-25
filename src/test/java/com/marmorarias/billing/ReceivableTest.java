package com.marmorarias.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marmorarias.billing.adapter.persistence.PaymentEntity;
import com.marmorarias.billing.adapter.persistence.ReceivableEntity;
import com.marmorarias.billing.adapter.persistence.ReceivableListItem;
import com.marmorarias.billing.application.BillingReceivableService;
import com.marmorarias.billing.domain.InstallmentStatus;
import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** BillingReceivableService: geração de parcelas, baixa de pagamento e isolamento por org. */
class ReceivableTest extends AbstractIntegrationTest {

    @Autowired
    private BillingReceivableService billingService;
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
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Billing Test");
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

    private List<ReceivableListItem.Parcela> parcelasDe(UUID receivableId) {
        return billingService.listar(tenant).stream()
                .filter(r -> r.id().equals(receivableId))
                .findFirst().orElseThrow().installments();
    }

    @Test
    void criarGeraParcelasQueSomamOValorTotal() {
        ReceivableEntity receivable = billingService.criar(tenant, orderId, new BigDecimal("1000.00"), 3,
                LocalDate.of(2026, 1, 10));

        List<ReceivableListItem.Parcela> parcelas = parcelasDe(receivable.getId());
        assertEquals(3, parcelas.size());

        BigDecimal soma = parcelas.stream().map(ReceivableListItem.Parcela::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("1000.00").compareTo(soma));
        assertEquals(true, parcelas.stream().allMatch(p -> p.status() == InstallmentStatus.PENDENTE));
    }

    @Test
    void registrarPagamentoMarcaParcelaComoPaga() {
        ReceivableEntity receivable = billingService.criar(tenant, orderId, new BigDecimal("600.00"), 2,
                LocalDate.of(2026, 2, 1));
        UUID installmentId = parcelasDe(receivable.getId()).get(0).id();

        PaymentEntity pagamento = billingService.registrarPagamento(tenant, installmentId, new BigDecimal("300.00"),
                "PIX");

        assertEquals(0, new BigDecimal("300.00").compareTo(pagamento.getValor()));
        assertEquals(InstallmentStatus.PAGO,
                parcelasDe(receivable.getId()).stream().filter(p -> p.id().equals(installmentId)).findFirst()
                        .orElseThrow().status());
    }

    @Test
    void darBaixaUsaOValorDaPropriaParcela() {
        ReceivableEntity receivable = billingService.criar(tenant, orderId, new BigDecimal("500.00"), 1,
                LocalDate.of(2026, 3, 1));
        UUID installmentId = parcelasDe(receivable.getId()).get(0).id();

        PaymentEntity pagamento = billingService.darBaixa(tenant, installmentId, "BOLETO");

        assertEquals(0, new BigDecimal("500.00").compareTo(pagamento.getValor()));
        assertEquals(InstallmentStatus.PAGO,
                parcelasDe(receivable.getId()).stream().filter(p -> p.id().equals(installmentId)).findFirst()
                        .orElseThrow().status());
    }

    @Test
    void registrarPagamentoDeParcelaInexistenteFalha() {
        assertThrows(NoSuchElementException.class,
                () -> billingService.registrarPagamento(tenant, UUID.randomUUID(), BigDecimal.TEN, "PIX"));
    }

    @Test
    void listarPorPedidoNaoVazaEntreOrganizacoes() {
        billingService.criar(tenant, orderId, new BigDecimal("400.00"), 1, LocalDate.of(2026, 4, 1));

        UUID outraOrgId = fixtures.criarOrganizacao("Outra Marmoraria");
        UUID outroUserId = fixtures.criarUsuario(outraOrgId, "admin");
        TenantContext outroTenant = new TenantContext(outraOrgId, outroUserId, Role.admin);

        assertEquals(true, billingService.listarPorPedido(outroTenant, orderId).isEmpty());
        assertEquals(false, billingService.listarPorPedido(tenant, orderId).isEmpty());
    }
}
