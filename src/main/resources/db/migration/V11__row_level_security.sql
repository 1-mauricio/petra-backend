-- Role de aplicação: sem BYPASSRLS, não-owner das tabelas. A senha é definida
-- fora desta migration (ALTER ROLE ... PASSWORD via secret manager), nunca em código.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_user') THEN
        CREATE ROLE app_user LOGIN NOBYPASSRLS;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO app_user;

-- organization é a raiz do tenant: isola por id em vez de organization_id.
ALTER TABLE organization ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_isolamento ON organization
    USING (id = current_setting('app.current_org_id')::uuid)
    WITH CHECK (id = current_setting('app.current_org_id')::uuid);

-- Demais tabelas org-scoped: policy padrão por organization_id.
DO $$
DECLARE
    v_tabela text;
BEGIN
    FOREACH v_tabela IN ARRAY ARRAY[
        'user_profile', 'org_settings', 'customer', 'lead', 'material', 'catalog_item',
        'quote', 'quote_version', 'quote_line_item', 'customer_order',
        'measurement', 'measurement_piece', 'production_task', 'delivery',
        'receivable', 'installment', 'payment', 'stage_transition', 'audit_log'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', v_tabela);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', v_tabela);
        EXECUTE format(
            'CREATE POLICY %I ON %I USING (organization_id = current_setting(''app.current_org_id'')::uuid) WITH CHECK (organization_id = current_setting(''app.current_org_id'')::uuid)',
            v_tabela || '_isolamento', v_tabela
        );
    END LOOP;
END
$$;

-- Grants de domínio: CRUD completo para app_user...
DO $$
DECLARE
    v_tabela text;
BEGIN
    FOREACH v_tabela IN ARRAY ARRAY[
        'organization', 'user_profile', 'org_settings', 'customer', 'lead', 'material', 'catalog_item',
        'quote', 'quote_version', 'quote_line_item', 'customer_order',
        'measurement', 'measurement_piece', 'production_task', 'delivery',
        'receivable', 'installment', 'payment'
    ] LOOP
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I TO app_user', v_tabela);
    END LOOP;
END
$$;

-- ...exceto nas tabelas append-only, onde UPDATE/DELETE são revogados.
GRANT SELECT, INSERT ON stage_transition TO app_user;
GRANT SELECT, INSERT ON audit_log TO app_user;
REVOKE UPDATE, DELETE ON stage_transition FROM app_user;
REVOKE UPDATE, DELETE ON audit_log FROM app_user;
