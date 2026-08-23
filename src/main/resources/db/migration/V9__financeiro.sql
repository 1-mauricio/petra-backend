CREATE TABLE receivable (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    order_id        uuid NOT NULL REFERENCES customer_order (id),
    valor_total     numeric(14,2) NOT NULL CHECK (valor_total >= 0),
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_receivable_org ON receivable (organization_id);
CREATE INDEX idx_receivable_order ON receivable (order_id);

CREATE TABLE installment (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    receivable_id   uuid NOT NULL REFERENCES receivable (id),
    numero          int NOT NULL CHECK (numero > 0),
    valor           numeric(12,2) NOT NULL CHECK (valor >= 0),
    vencimento      date NOT NULL,
    status          installment_status NOT NULL DEFAULT 'PENDENTE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_installment_receivable_numero UNIQUE (receivable_id, numero)
);

CREATE INDEX idx_installment_org ON installment (organization_id);
CREATE INDEX idx_installment_receivable ON installment (receivable_id);

CREATE TRIGGER trg_installment_updated_at
    BEFORE UPDATE ON installment
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TABLE payment (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    installment_id  uuid NOT NULL REFERENCES installment (id),
    valor           numeric(12,2) NOT NULL CHECK (valor > 0),
    data_pagamento  timestamptz NOT NULL DEFAULT now(),
    forma_pagamento text NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_org ON payment (organization_id);
CREATE INDEX idx_payment_installment ON payment (installment_id);
