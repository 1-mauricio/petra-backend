package com.marmorarias.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para testes de integração com Postgres real via Testcontainers, RLS ativa de fato: o
 * Flyway roda como owner (superusuário do container), mas o datasource da aplicação conecta como
 * app_user — a mesma role NOBYPASSRLS que roda em produção — para que os testes provem a RLS, não
 * só a lógica de aplicação. app_user precisa de senha (V11 cria a role sem uma, de propósito, para
 * ser setada fora das migrations); aqui setamos antes do Flyway rodar.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.org/.well-known/jwks.json"
})
public abstract class AbstractIntegrationTest {

    protected static final String APP_USER_PASSWORD = "app_user_test_password";

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()); Statement statement = connection.createStatement()) {
            statement.execute("""
                    DO $$
                    BEGIN
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_user') THEN
                            CREATE ROLE app_user LOGIN NOBYPASSRLS PASSWORD '%s';
                        END IF;
                    END
                    $$;
                    """.formatted(APP_USER_PASSWORD));
            // Em produção essas roles são provisionadas pelo próprio Supabase; aqui simulamos sua
            // existência para as migrations (V15, V27) que dão GRANT/policy a elas funcionarem.
            for (String role : new String[] {"supabase_auth_admin", "anon", "authenticated"}) {
                statement.execute("""
                        DO $$
                        BEGIN
                            IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '%s') THEN
                                CREATE ROLE %s NOLOGIN;
                            END IF;
                        END
                        $$;
                        """.formatted(role, role));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao preparar role app_user no Postgres de teste", e);
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "app_user");
        registry.add("spring.datasource.password", () -> APP_USER_PASSWORD);
    }

    @BeforeAll
    static void ensureContainerRunning() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }
}
