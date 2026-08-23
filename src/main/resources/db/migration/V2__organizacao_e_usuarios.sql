CREATE TABLE organization (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        text NOT NULL,
    cnpj        text,
    created_at  timestamptz NOT NULL DEFAULT now()
);

-- Espelha o usuário do Supabase Auth: id = auth.users.id, sem senha/local login.
CREATE TABLE user_profile (
    id              uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organization (id),
    role            user_role NOT NULL,
    nome            text NOT NULL,
    email           text NOT NULL,
    ativo           boolean NOT NULL DEFAULT true,
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_profile_org ON user_profile (organization_id);

CREATE TABLE org_settings (
    organization_id     uuid PRIMARY KEY REFERENCES organization (id),
    tolerancia_perc      numeric(5,2) NOT NULL DEFAULT 5.00,
    tolerancia_abs       numeric(12,2) NOT NULL DEFAULT 500.00,
    fator_perda          jsonb NOT NULL DEFAULT '{}'::jsonb,
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_org_settings_updated_at
    BEFORE UPDATE ON org_settings
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
