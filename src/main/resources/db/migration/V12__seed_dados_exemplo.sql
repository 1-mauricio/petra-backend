-- Seed de exemplo para desenvolvimento local. NÃO aplicar em produção.
-- SET LOCAL garante que o WITH CHECK das policies de RLS seja satisfeito
-- durante os INSERTs desta própria migration (que roda como role owner).
SET LOCAL app.current_org_id = '00000000-0000-0000-0000-000000000001';

INSERT INTO organization (id, nome, cnpj) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Marmoraria Exemplo Ltda', '11222333000181');

INSERT INTO org_settings (organization_id) VALUES
    ('00000000-0000-0000-0000-000000000001');

-- Ids fixos simulando usuários já criados no Supabase Auth (auth.users.id).
INSERT INTO user_profile (id, organization_id, role, nome, email) VALUES
    ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000001', 'admin', 'Ana Admin', 'ana.admin@exemplo.com'),
    ('00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-000000000001', 'comercial', 'Carlos Comercial', 'carlos.comercial@exemplo.com'),
    ('00000000-0000-0000-0000-0000000000a3', '00000000-0000-0000-0000-000000000001', 'producao', 'Paula Produção', 'paula.producao@exemplo.com');

INSERT INTO material (organization_id, tipo, cor, preco_m2, largura_chapa, comprimento_chapa) VALUES
    ('00000000-0000-0000-0000-000000000001', 'GRANITO', 'Preto São Gabriel', 480.00, 2.95, 1.95),
    ('00000000-0000-0000-0000-000000000001', 'MARMORE', 'Branco Carrara', 620.00, 3.00, 1.90),
    ('00000000-0000-0000-0000-000000000001', 'GRANITO', 'Verde Ubatuba', 410.00, 2.90, 1.95);

INSERT INTO catalog_item (organization_id, tipo, descricao, unidade, preco) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ACABAMENTO', 'Borda Bisote 3cm', 'METRO_LINEAR', 45.00),
    ('00000000-0000-0000-0000-000000000001', 'RECORTE', 'Recorte para cuba', 'UNIDADE', 120.00),
    ('00000000-0000-0000-0000-000000000001', 'MAO_DE_OBRA', 'Instalação (hora técnica)', 'HORA', 90.00);
