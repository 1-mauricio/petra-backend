package com.marmorarias.quoting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.quoting.adapter.persistence.QuoteLineItemEntity;
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

/** Invariante 4: preço de catálogo é snapshotado na linha do orçamento — reajuste posterior não muda orçamento já criado. */
class QuoteSnapshotTest extends AbstractIntegrationTest {

    @Autowired
    private QuoteService quoteService;
    @Autowired
    private DataSource dataSource;

    @Test
    void reajusteDeCatalogoNaoAlteraOrcamentoJaCriado() {
        TestFixtures fixtures = new TestFixtures(dataSource);
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Snapshot Test");
        UUID userId = fixtures.criarUsuario(orgId, "admin");
        TenantContext tenant = new TenantContext(orgId, userId, Role.admin);
        UUID customerId = fixtures.criarCustomer(orgId);
        UUID materialId = fixtures.criarMaterial(orgId, new BigDecimal("500.00"));

        CriarOrcamentoRequest request = new CriarOrcamentoRequest(customerId, null,
                List.of(new PecaRequest(materialId, new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of())),
                Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        QuoteVersionEntity versao = quoteService.criarOrcamento(tenant, request);
        BigDecimal totalOriginal = versao.getValorTotal();

        List<QuoteLineItemEntity> linhasAntes = quoteService.buscarLinhas(tenant, versao.getId());
        assertEquals(0, new BigDecimal("500.00").compareTo(linhasAntes.get(0).getPrecoUnitarioSnapshot()));

        fixtures.atualizarPrecoMaterial(orgId, materialId, new BigDecimal("900.00"));

        QuoteVersionEntity versaoDepois = quoteService.buscarVersao(tenant, versao.getId());
        List<QuoteLineItemEntity> linhasDepois = quoteService.buscarLinhas(tenant, versao.getId());

        assertEquals(0, totalOriginal.compareTo(versaoDepois.getValorTotal()));
        assertEquals(0, new BigDecimal("500.00").compareTo(linhasDepois.get(0).getPrecoUnitarioSnapshot()));
    }
}
