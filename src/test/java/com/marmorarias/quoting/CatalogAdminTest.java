package com.marmorarias.quoting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.quoting.adapter.persistence.MaterialEntity;
import com.marmorarias.quoting.application.QuoteService;
import com.marmorarias.support.AbstractIntegrationTest;
import com.marmorarias.support.TestFixtures;
import java.math.BigDecimal;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Cadastro/reajuste de catálogo — CRUD simples, sem invariante própria além do snapshot já coberto em QuoteSnapshotTest. */
class CatalogAdminTest extends AbstractIntegrationTest {

    @Autowired
    private QuoteService quoteService;
    @Autowired
    private DataSource dataSource;

    @Test
    void criaEAtualizaMaterial() {
        TestFixtures fixtures = new TestFixtures(dataSource);
        UUID orgId = fixtures.criarOrganizacao("Marmoraria Catalog Test");
        UUID userId = fixtures.criarUsuario(orgId, "admin");
        TenantContext tenant = new TenantContext(orgId, userId, Role.admin);

        MaterialEntity criado = quoteService.criarMaterial(tenant, "Granito", "Preto São Gabriel",
                new BigDecimal("600.00"), new BigDecimal("3.0"), new BigDecimal("1.8"));
        assertTrue(criado.isAtivo());
        assertEquals(0, new BigDecimal("600.00").compareTo(criado.getPrecoM2()));

        MaterialEntity atualizado = quoteService.atualizarMaterial(tenant, criado.getId(), new BigDecimal("650.00"), false);
        assertEquals(0, new BigDecimal("650.00").compareTo(atualizado.getPrecoM2()));
        assertFalse(atualizado.isAtivo());
    }
}
