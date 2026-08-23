-- Assinatura SaaS da organização (billing da plataforma, via Stripe).
-- Não confundir com o financeiro do cliente final da marmoraria (receivable/
-- installment/payment em V9) — contextos separados.

CREATE TYPE org_billing_status AS ENUM ('SEM_ASSINATURA', 'ATIVA', 'INADIMPLENTE', 'CANCELADA');

ALTER TABLE organization
    ADD COLUMN stripe_customer_id text UNIQUE,
    ADD COLUMN billing_status org_billing_status NOT NULL DEFAULT 'SEM_ASSINATURA';

CREATE TABLE subscription (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id       uuid NOT NULL REFERENCES organization (id),
    stripe_subscription_id text NOT NULL UNIQUE,
    stripe_price_id        text NOT NULL,
    stripe_status           text NOT NULL,
    current_period_end      timestamptz,
    created_at               timestamptz NOT NULL DEFAULT now(),
    updated_at               timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_subscription_org UNIQUE (organization_id)
);

CREATE INDEX idx_subscription_org ON subscription (organization_id);

CREATE TRIGGER trg_subscription_updated_at
    BEFORE UPDATE ON subscription
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

ALTER TABLE subscription ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscription FORCE ROW LEVEL SECURITY;
CREATE POLICY subscription_isolamento ON subscription
    USING (organization_id = current_setting('app.current_org_id')::uuid)
    WITH CHECK (organization_id = current_setting('app.current_org_id')::uuid);

GRANT SELECT, INSERT, UPDATE ON subscription TO app_user;

-- Registro de eventos de webhook do Stripe já processados, para idempotência
-- por event_id. Tabela de plataforma (não é dado de tenant) — sem RLS por
-- organization_id, só é lida/escrita pelo endpoint de webhook.
CREATE TABLE billing_webhook_event (
    stripe_event_id text PRIMARY KEY,
    event_type      text NOT NULL,
    processed_at    timestamptz NOT NULL DEFAULT now()
);

GRANT SELECT, INSERT ON billing_webhook_event TO app_user;
REVOKE UPDATE, DELETE ON billing_webhook_event FROM app_user;
