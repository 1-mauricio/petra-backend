-- Append-only: histórico de transições de estado do pedido.
CREATE TABLE stage_transition (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    order_id        uuid NOT NULL REFERENCES customer_order (id),
    from_state      order_state,
    to_state        order_state NOT NULL,
    actor           uuid REFERENCES user_profile (id),
    motivo          text,
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_stage_transition_org ON stage_transition (organization_id);
CREATE INDEX idx_stage_transition_order ON stage_transition (order_id);

-- Append-only: trilha de auditoria genérica.
CREATE TABLE audit_log (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    actor           uuid REFERENCES user_profile (id),
    entidade        text NOT NULL,
    entidade_id     uuid NOT NULL,
    acao            text NOT NULL,
    dados_anteriores jsonb,
    dados_novos     jsonb,
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_org ON audit_log (organization_id);
CREATE INDEX idx_audit_log_entidade ON audit_log (entidade, entidade_id);
