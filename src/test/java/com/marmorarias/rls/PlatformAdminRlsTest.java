package com.marmorarias.rls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.marmorarias.support.AbstractIntegrationTest;
import com.marmorarias.support.TestFixtures;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bypass de RLS do admin da plataforma (V24): app.is_platform_admin=true libera leitura
 * cross-org; sem nenhuma GUC setada (nem current_org_id, nem is_platform_admin), nenhuma linha
 * é visível — prova que o current_setting(..., true) (missing_ok) introduzido em V24 não afrouxou
 * a RLS para quem não seta a flag.
 */
class PlatformAdminRlsTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void semGucNenhumaNaoLeCustomerDeNenhumaOrg() throws SQLException {
        TestFixtures fixtures = new TestFixtures(dataSource);
        UUID orgA = fixtures.criarOrganizacao("Marmoraria A");
        fixtures.criarCustomer(orgA);

        assertEquals(0, contarCustomers(false));
    }

    @Test
    void comIsPlatformAdminLeCustomerDeTodasAsOrgs() throws SQLException {
        TestFixtures fixtures = new TestFixtures(dataSource);
        UUID orgA = fixtures.criarOrganizacao("Marmoraria A");
        UUID orgB = fixtures.criarOrganizacao("Marmoraria B");
        fixtures.criarCustomer(orgA);
        fixtures.criarCustomer(orgB);

        assertEquals(2, contarCustomers(true));
    }

    private long contarCustomers(boolean comoPlatformAdmin) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            if (comoPlatformAdmin) {
                try (PreparedStatement setFlag =
                        con.prepareStatement("SELECT set_config('app.is_platform_admin', 'true', true)")) {
                    setFlag.executeQuery();
                }
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT count(*) FROM customer");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                long total = rs.getLong(1);
                con.commit();
                return total;
            }
        }
    }
}
