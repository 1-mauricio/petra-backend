package com.marmorarias.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.marmorarias.channels.NotificationPort;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Canal externo (NotificationPort) nunca pode derrubar a transição do pedido — ver
 * OrderService.notificarCliente. Mocka o port pra simular falha de envio (SMTP fora do ar, etc.)
 * e prova que o try/catch realmente segura isso.
 */
class OrderNotificationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private QuoteService quoteService;
    @Autowired
    private DataSource dataSource;
    @MockBean
    private NotificationPort notificationPort;

    @Test
    void falhaNoEnvioDeNotificacaoNaoBloqueiaATransicao() {
        doThrow(new RuntimeException("smtp indisponível")).when(notificationPort).notificar(any(), any());

        TestFixtures fixtures = new TestFixtures(dataSource);
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Notificacao Test");
        UUID userId = fixtures.criarUsuario(orgId, "admin");
        TenantContext tenant = new TenantContext(orgId, userId, Role.admin);
        UUID customerId = fixtures.criarCustomer(orgId);
        UUID materialId = fixtures.criarMaterial(orgId, new BigDecimal("500.00"));

        CriarOrcamentoRequest request = new CriarOrcamentoRequest(customerId, null,
                List.of(new PecaRequest(materialId, new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of())),
                Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        QuoteVersionEntity versao = quoteService.criarOrcamento(tenant, request);
        quoteService.enviar(tenant, versao.getId());
        quoteService.aprovar(tenant, versao.getId());
        CustomerOrderEntity order = orderService.criarPedido(tenant, customerId, versao.getId());

        CustomerOrderEntity transicionado = orderService.transicionar(tenant, order.getId(), OrderState.APROVACAO,
                "aprovado pelo cliente");

        assertEquals(OrderState.APROVACAO, transicionado.getState());
        verify(notificationPort).notificar(any(), any());
    }
}
