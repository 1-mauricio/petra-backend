-- Seed adicional de dev/teste: cobre o pipeline inteiro (lead → orçamento → pedido →
-- medição → produção → entrega → financeiro) para exercitar a UI e os invariantes de negócio.
-- Mesma ressalva de V12: NÃO aplicar em produção.
SET LOCAL app.current_org_id = '00000000-0000-0000-0000-000000000001';

INSERT INTO customer (id, organization_id, tipo, nome, cpf_cnpj, email, telefone) VALUES
    ('00000000-0000-0000-0000-0000000000c1', '00000000-0000-0000-0000-000000000001', 'PF', 'João Cliente', '12345678909', 'joao.cliente@exemplo.com', '11988887777'),
    ('00000000-0000-0000-0000-0000000000c2', '00000000-0000-0000-0000-000000000001', 'PF', 'Maria Compradora', '98765432100', 'maria.compradora@exemplo.com', '11977776666'),
    ('00000000-0000-0000-0000-0000000000c3', '00000000-0000-0000-0000-000000000001', 'PJ', 'Reforma Rápida Ltda', '12345678000195', 'contato@reformarapida.com', '1130001111');

INSERT INTO lead (id, organization_id, customer_id, origem, status) VALUES
    ('00000000-0000-0000-0000-0000000000e1', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000c1', 'Instagram', 'GANHO'),
    ('00000000-0000-0000-0000-0000000000e2', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000c3', 'Indicação', 'EM_NEGOCIACAO');

-- Pedido 1 (cliente João): já concluído, com financeiro quitado.
INSERT INTO quote (id, organization_id, customer_id, lead_id) VALUES
    ('00000000-0000-0000-0000-0000000000f1', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000c1', '00000000-0000-0000-0000-0000000000e1');
INSERT INTO quote_version (id, organization_id, quote_id, version_number, status, valor_total, created_by, approved_at) VALUES
    ('00000000-0000-0000-0000-00000000f101', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000f1', 1, 'APROVADO', 2350.00, '00000000-0000-0000-0000-0000000000a2', now() - interval '30 days');
INSERT INTO quote_line_item (organization_id, quote_version_id, material_id, descricao, quantidade, unidade, preco_unitario_snapshot, subtotal)
    SELECT '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-00000000f101', id, 'Bancada cozinha - ' || cor, 4.5, 'METRO_QUADRADO', preco_m2, preco_m2 * 4.5
    FROM material WHERE organization_id = '00000000-0000-0000-0000-000000000001' AND cor = 'Preto São Gabriel';

INSERT INTO customer_order (id, organization_id, customer_id, current_quote_version_id, state) VALUES
    ('00000000-0000-0000-0000-00000000d001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000c1', '00000000-0000-0000-0000-00000000f101', 'ORCAMENTO');

INSERT INTO measurement (id, organization_id, order_id, status, tecnico_responsavel, approved_at) VALUES
    ('00000000-0000-0000-0000-0000000b0001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-00000000d001', 'APROVADO', '00000000-0000-0000-0000-0000000000a3', now() - interval '25 days');
INSERT INTO measurement_piece (organization_id, measurement_id, material_id, descricao, largura_m, comprimento_m, quantidade)
    SELECT '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000b0001', id, 'Bancada cozinha', 0.65, 6.9, 1
    FROM material WHERE organization_id = '00000000-0000-0000-0000-000000000001' AND cor = 'Preto São Gabriel';

UPDATE customer_order SET state = 'PRODUCAO' WHERE id = '00000000-0000-0000-0000-00000000d001';
UPDATE customer_order SET state = 'ENTREGA' WHERE id = '00000000-0000-0000-0000-00000000d001';
UPDATE customer_order SET state = 'INSTALACAO' WHERE id = '00000000-0000-0000-0000-00000000d001';
UPDATE customer_order SET state = 'CONCLUIDO' WHERE id = '00000000-0000-0000-0000-00000000d001';

INSERT INTO production_task (organization_id, order_id, descricao, status, responsavel) VALUES
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-00000000d001', 'Corte e polimento', 'CONCLUIDA', '00000000-0000-0000-0000-0000000000a3');
INSERT INTO delivery (organization_id, order_id, status, data_agendada, data_entrega) VALUES
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-00000000d001', 'ENTREGUE', now() - interval '5 days', now() - interval '5 days');

INSERT INTO receivable (id, organization_id, order_id, valor_total) VALUES
    ('00000000-0000-0000-0000-0000000a1001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-00000000d001', 2350.00);
INSERT INTO installment (id, organization_id, receivable_id, numero, valor, vencimento, status) VALUES
    ('00000000-0000-0000-0000-0000000a2001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000a1001', 1, 2350.00, current_date - 5, 'PAGO');
INSERT INTO payment (organization_id, installment_id, valor, forma_pagamento) VALUES
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000a2001', 2350.00, 'PIX');

-- Pedido 2 (cliente Maria): em produção, medição já aprovada.
INSERT INTO quote (id, organization_id, customer_id) VALUES
    ('00000000-0000-0000-0000-0000000000f2', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000c2');
INSERT INTO quote_version (id, organization_id, quote_id, version_number, status, valor_total, created_by, approved_at) VALUES
    ('00000000-0000-0000-0000-00000000f201', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000f2', 1, 'APROVADO', 1860.00, '00000000-0000-0000-0000-0000000000a2', now() - interval '10 days');
INSERT INTO quote_line_item (organization_id, quote_version_id, material_id, descricao, quantidade, unidade, preco_unitario_snapshot, subtotal)
    SELECT '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-00000000f201', id, 'Soleira e peitoril - ' || cor, 3.0, 'METRO_QUADRADO', preco_m2, preco_m2 * 3.0
    FROM material WHERE organization_id = '00000000-0000-0000-0000-000000000001' AND cor = 'Verde Ubatuba';

INSERT INTO customer_order (id, organization_id, customer_id, current_quote_version_id, state) VALUES
    ('00000000-0000-0000-0000-00000000d002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000c2', '00000000-0000-0000-0000-00000000f201', 'ORCAMENTO');

INSERT INTO measurement (id, organization_id, order_id, status, tecnico_responsavel, approved_at) VALUES
    ('00000000-0000-0000-0000-0000000b0002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-00000000d002', 'APROVADO', '00000000-0000-0000-0000-0000000000a3', now() - interval '3 days');
INSERT INTO measurement_piece (organization_id, measurement_id, material_id, descricao, largura_m, comprimento_m, quantidade)
    SELECT '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000b0002', id, 'Soleira', 0.3, 10, 1
    FROM material WHERE organization_id = '00000000-0000-0000-0000-000000000001' AND cor = 'Verde Ubatuba';

UPDATE customer_order SET state = 'PRODUCAO' WHERE id = '00000000-0000-0000-0000-00000000d002';
INSERT INTO production_task (organization_id, order_id, descricao, status, responsavel) VALUES
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-00000000d002', 'Corte e polimento', 'EM_ANDAMENTO', '00000000-0000-0000-0000-0000000000a3');

-- Pedido 3 (cliente Reforma Rápida): ainda em orçamento, sem medição — cobre o guard da invariante 1.
INSERT INTO quote (id, organization_id, customer_id, lead_id) VALUES
    ('00000000-0000-0000-0000-0000000000f3', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000c3', '00000000-0000-0000-0000-0000000000e2');
INSERT INTO quote_version (id, organization_id, quote_id, version_number, status, valor_total, created_by) VALUES
    ('00000000-0000-0000-0000-00000000f301', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000f3', 1, 'ENVIADO', 980.00, '00000000-0000-0000-0000-0000000000a2');
INSERT INTO quote_line_item (organization_id, quote_version_id, catalog_item_id, descricao, quantidade, unidade, preco_unitario_snapshot, subtotal)
    SELECT '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-00000000f301', id, descricao, 1, unidade, preco, preco
    FROM catalog_item WHERE organization_id = '00000000-0000-0000-0000-000000000001' AND descricao = 'Instalação (hora técnica)';
