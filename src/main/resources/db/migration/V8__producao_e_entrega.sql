CREATE TABLE production_task (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    order_id        uuid NOT NULL REFERENCES customer_order (id),
    descricao       text NOT NULL,
    status          production_task_status NOT NULL DEFAULT 'PENDENTE',
    responsavel     uuid REFERENCES user_profile (id),
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_production_task_org ON production_task (organization_id);
CREATE INDEX idx_production_task_order ON production_task (order_id);

CREATE TRIGGER trg_production_task_updated_at
    BEFORE UPDATE ON production_task
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TABLE delivery (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    order_id        uuid NOT NULL REFERENCES customer_order (id),
    status          delivery_status NOT NULL DEFAULT 'AGENDADA',
    data_agendada   timestamptz,
    data_entrega    timestamptz,
    endereco        jsonb,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_delivery_org ON delivery (organization_id);
CREATE INDEX idx_delivery_order ON delivery (order_id);

CREATE TRIGGER trg_delivery_updated_at
    BEFORE UPDATE ON delivery
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
