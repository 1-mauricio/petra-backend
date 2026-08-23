CREATE TABLE customer (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    tipo            customer_tipo NOT NULL,
    nome            text NOT NULL,
    cpf_cnpj        text NOT NULL,
    email           text,
    telefone        text,
    endereco        jsonb,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_customer_cpf_cnpj_valido CHECK (fn_validar_cpf_cnpj(tipo, cpf_cnpj)),
    CONSTRAINT uq_customer_org_cpf_cnpj UNIQUE (organization_id, cpf_cnpj)
);

CREATE INDEX idx_customer_org ON customer (organization_id);

CREATE TRIGGER trg_customer_updated_at
    BEFORE UPDATE ON customer
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TABLE lead (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    customer_id     uuid REFERENCES customer (id),
    origem          text,
    status          lead_status NOT NULL DEFAULT 'ABERTO',
    motivo_perda    text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_lead_motivo_perda CHECK (status <> 'PERDIDO' OR motivo_perda IS NOT NULL)
);

CREATE INDEX idx_lead_org ON lead (organization_id);

CREATE TRIGGER trg_lead_updated_at
    BEFORE UPDATE ON lead
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
