-- Plano da assinatura (básico/pro) gravado explicitamente em vez de só
-- derivado do stripe_price_id em runtime. Default 'basico' (fail-safe do
-- lado mais restrito) backfila a única linha já existente.
ALTER TABLE subscription
    ADD COLUMN plano text NOT NULL DEFAULT 'basico';
