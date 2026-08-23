-- Segundo tenant de teste para verificar isolamento multi-tenant (RLS) em dev local.
-- NÃO aplicar em produção.
-- Ids de auth.users fixos abaixo já foram criados via Supabase Auth Admin API
-- (mesmo padrão de V12 — este schema de migration não tem grant em auth.*).
-- Senha de todos: senha123
SET LOCAL app.current_org_id = '00000000-0000-0000-0000-000000000002';

INSERT INTO organization (id, nome, cnpj, billing_status) VALUES
    ('00000000-0000-0000-0000-000000000002', 'Marmoraria Teste 2 Ltda', '22333444000199', 'ATIVA');

INSERT INTO org_settings (organization_id) VALUES
    ('00000000-0000-0000-0000-000000000002');

INSERT INTO user_profile (id, organization_id, role, nome, email) VALUES
    ('f63d667e-eecc-4b86-a2e4-36caf5cb94f0', '00000000-0000-0000-0000-000000000002', 'admin', 'Ana Teste', 'admin.teste@marmoraria2.dev'),
    ('a49ee817-85a4-4d5d-b4ed-21f182cedfba', '00000000-0000-0000-0000-000000000002', 'comercial', 'Beto Teste', 'comercial.teste@marmoraria2.dev'),
    ('5d69af7c-28ac-4f9c-af6c-320c910c06f2', '00000000-0000-0000-0000-000000000002', 'producao', 'Carla Teste', 'producao.teste@marmoraria2.dev');

INSERT INTO material (organization_id, tipo, cor, preco_m2, largura_chapa, comprimento_chapa) VALUES
    ('00000000-0000-0000-0000-000000000002', 'GRANITO', 'Cinza Corumbá', 350.00, 3.00, 1.95);

INSERT INTO catalog_item (organization_id, tipo, descricao, unidade, preco) VALUES
    ('00000000-0000-0000-0000-000000000002', 'ACABAMENTO', 'Borda Reta', 'METRO_LINEAR', 35.00);
