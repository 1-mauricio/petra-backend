-- Allowlist de staff Petra (admin da plataforma, sem organização própria). Tabela pequena e
-- confiável, sem RLS — não é dado de tenant. supabase_auth_admin precisa ler para o hook (V28).
CREATE TABLE platform_admin (
    user_id    uuid PRIMARY KEY,
    email      text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

GRANT SELECT ON platform_admin TO app_user;
GRANT SELECT ON platform_admin TO supabase_auth_admin;

-- Bypass de RLS para o admin da plataforma: quando a aplicação seta app.is_platform_admin=true
-- (só atrás de @PreAuthorize("hasRole('platform_admin')"), nunca por conta própria do usuário),
-- as policies liberam leitura/escrita cross-org. Mesmo app_user e mesma fronteira de confiança
-- do app.current_org_id (V11) — sem role/datasource novo.
--
-- current_setting(..., true) (missing_ok) substitui a forma sem esse argumento usada em V11/V13:
-- necessário porque agora uma sessão pode setar só uma das duas GUCs (tenant comum nunca seta
-- is_platform_admin; admin da plataforma não seta current_org_id em leitura cross-org) — sem
-- missing_ok, a GUC ausente lançaria erro em vez de simplesmente não casar a condição.

-- organization é a raiz do tenant: isola por id em vez de organization_id.
DROP POLICY organization_isolamento ON organization;
CREATE POLICY organization_isolamento ON organization
    USING (id = current_setting('app.current_org_id', true)::uuid
        OR current_setting('app.is_platform_admin', true) = 'true')
    WITH CHECK (id = current_setting('app.current_org_id', true)::uuid
        OR current_setting('app.is_platform_admin', true) = 'true');

-- Demais tabelas org-scoped (V11) + subscription (V13): recria a policy com o bypass.
DO $$
DECLARE
    v_tabela text;
BEGIN
    FOREACH v_tabela IN ARRAY ARRAY[
        'user_profile', 'org_settings', 'customer', 'lead', 'material', 'catalog_item',
        'quote', 'quote_version', 'quote_line_item', 'customer_order',
        'measurement', 'measurement_piece', 'production_task', 'delivery',
        'receivable', 'installment', 'payment', 'stage_transition', 'audit_log',
        'subscription'
    ] LOOP
        EXECUTE format('DROP POLICY %I ON %I', v_tabela || '_isolamento', v_tabela);
        EXECUTE format(
            'CREATE POLICY %I ON %I USING (organization_id = current_setting(''app.current_org_id'', true)::uuid OR current_setting(''app.is_platform_admin'', true) = ''true'') WITH CHECK (organization_id = current_setting(''app.current_org_id'', true)::uuid OR current_setting(''app.is_platform_admin'', true) = ''true'')',
            v_tabela || '_isolamento', v_tabela
        );
    END LOOP;
END
$$;
