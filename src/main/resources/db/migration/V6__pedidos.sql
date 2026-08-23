-- "order" é palavra reservada em SQL; a entidade PEDIDO vive na tabela customer_order.
CREATE TABLE customer_order (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id         uuid NOT NULL REFERENCES organization (id),
    customer_id             uuid NOT NULL REFERENCES customer (id),
    current_quote_version_id uuid NOT NULL REFERENCES quote_version (id),
    state                   order_state NOT NULL DEFAULT 'ORCAMENTO',
    valor_taxa_cancelamento numeric(12,2),
    motivo_cancelamento     text,
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_customer_order_org ON customer_order (organization_id);
CREATE INDEX idx_customer_order_quote_version ON customer_order (current_quote_version_id);

CREATE TRIGGER trg_customer_order_updated_at
    BEFORE UPDATE ON customer_order
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Invariante 1: PRODUCAO só é alcançável com measurement APROVADO do pedido
-- e com a quote_version corrente já APROVADA (ou seja, sem revisão de orçamento pendente).
CREATE OR REPLACE FUNCTION fn_customer_order_guard_producao() RETURNS trigger AS $$
DECLARE
    v_quote_version_status quote_version_status;
BEGIN
    IF NEW.state = 'PRODUCAO' AND OLD.state IS DISTINCT FROM 'PRODUCAO' THEN
        IF NOT EXISTS (
            SELECT 1 FROM measurement
            WHERE order_id = NEW.id AND status = 'APROVADO'
        ) THEN
            RAISE EXCEPTION 'pedido % não pode ir para PRODUCAO: nenhuma medição APROVADO', NEW.id;
        END IF;

        SELECT status INTO v_quote_version_status
        FROM quote_version WHERE id = NEW.current_quote_version_id;

        IF v_quote_version_status IS DISTINCT FROM 'APROVADO' THEN
            RAISE EXCEPTION 'pedido % não pode ir para PRODUCAO: revisão de orçamento pendente', NEW.id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_customer_order_guard_producao
    BEFORE UPDATE ON customer_order
    FOR EACH ROW EXECUTE FUNCTION fn_customer_order_guard_producao();
