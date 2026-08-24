package com.marmorarias.support;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

/**
 * Semeia dados de teste passando pelo mesmo caminho RLS que a aplicação (SET LOCAL
 * app.current_org_id antes de cada INSERT), em vez de inserir direto como owner — assim os
 * fixtures também provam que app_user consegue escrever nos próprios dados.
 */
public class TestFixtures {

    private static final String CNPJ_VALIDO = "11222333000181";

    private final DataSource dataSource;

    public TestFixtures(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public UUID criarOrganizacao(String nome) {
        UUID id = UUID.randomUUID();
        executarNaOrg(id, con -> {
            executar(con, "INSERT INTO organization (id, nome, cnpj) VALUES (?, ?, ?)", id, nome, CNPJ_VALIDO);
            executar(con, "INSERT INTO org_settings (organization_id) VALUES (?)", id);
        });
        return id;
    }

    public void ajustarTolerancias(UUID orgId, BigDecimal toleranciaPerc, BigDecimal toleranciaAbs) {
        executarNaOrg(orgId, con -> executar(con,
                "UPDATE org_settings SET tolerancia_perc = ?, tolerancia_abs = ? WHERE organization_id = ?",
                toleranciaPerc, toleranciaAbs, orgId));
    }

    public void ajustarLimiteDesconto(UUID orgId, BigDecimal descontoLimitePerc) {
        executarNaOrg(orgId, con -> executar(con,
                "UPDATE org_settings SET desconto_limite_perc = ? WHERE organization_id = ?",
                descontoLimitePerc, orgId));
    }

    public UUID criarUsuario(UUID orgId, String role) {
        UUID id = UUID.randomUUID();
        executarNaOrg(orgId, con -> executar(con,
                "INSERT INTO user_profile (id, organization_id, role, nome, email) VALUES (?, ?, ?::user_role, ?, ?)",
                id, orgId, role, "Usuário " + role, role + "@teste.com"));
        return id;
    }

    public UUID criarCustomer(UUID orgId) {
        UUID id = UUID.randomUUID();
        executarNaOrg(orgId, con -> executar(con,
                "INSERT INTO customer (id, organization_id, tipo, nome, cpf_cnpj) VALUES (?, ?, 'PJ', 'Cliente Teste', ?)",
                id, orgId, CNPJ_VALIDO));
        return id;
    }

    public UUID criarMaterial(UUID orgId, BigDecimal precoM2) {
        UUID id = UUID.randomUUID();
        executarNaOrg(orgId, con -> executar(con,
                "INSERT INTO material (id, organization_id, tipo, cor, preco_m2, largura_chapa, comprimento_chapa) "
                        + "VALUES (?, ?, 'GRANITO', 'Cor Teste', ?, 3.0, 1.8)",
                id, orgId, precoM2));
        return id;
    }

    public String lerEstadoPedido(UUID orgId, UUID orderId) {
        String[] resultado = new String[1];
        executarNaOrg(orgId, con -> {
            try (PreparedStatement ps = con.prepareStatement("SELECT state FROM customer_order WHERE id = ?")) {
                ps.setObject(1, orderId);
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    resultado[0] = rs.getString(1);
                }
            }
        });
        return resultado[0];
    }

    public void atualizarPrecoMaterial(UUID orgId, UUID materialId, BigDecimal novoPrecoM2) {
        executarNaOrg(orgId, con -> executar(con, "UPDATE material SET preco_m2 = ? WHERE id = ?", novoPrecoM2,
                materialId));
    }

    public interface Acao {
        void executar(Connection connection) throws SQLException;
    }

    public void executarNaOrg(UUID orgId, Acao acao) {
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement setOrg = con.prepareStatement("SELECT set_config('app.current_org_id', ?, true)")) {
                setOrg.setString(1, orgId.toString());
                setOrg.executeQuery();
            }
            acao.executar(con);
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void executar(Connection con, String sql, Object... params) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
