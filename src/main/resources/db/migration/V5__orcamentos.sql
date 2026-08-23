CREATE TABLE quote (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    customer_id     uuid NOT NULL REFERENCES customer (id),
    lead_id         uuid REFERENCES lead (id),
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_quote_org ON quote (organization_id);

CREATE TABLE quote_version (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    quote_id        uuid NOT NULL REFERENCES quote (id),
    version_number  int NOT NULL CHECK (version_number > 0),
    status          quote_version_status NOT NULL DEFAULT 'RASCUNHO',
    valor_total     numeric(14,2) NOT NULL DEFAULT 0 CHECK (valor_total >= 0),
    created_by      uuid REFERENCES user_profile (id),
    created_at      timestamptz NOT NULL DEFAULT now(),
    approved_at     timestamptz,
    CONSTRAINT uq_quote_version_numero UNIQUE (quote_id, version_number)
);

CREATE INDEX idx_quote_version_org ON quote_version (organization_id);
CREATE INDEX idx_quote_version_quote ON quote_version (quote_id);

-- Orçamento aprovado nunca é sobrescrito: versão aprovada vira read-only.
CREATE OR REPLACE FUNCTION fn_quote_version_bloquear_update_aprovado() RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'APROVADO' THEN
        RAISE EXCEPTION 'quote_version % já aprovada é imutável', OLD.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_quote_version_imutavel_aprovado
    BEFORE UPDATE ON quote_version
    FOR EACH ROW EXECUTE FUNCTION fn_quote_version_bloquear_update_aprovado();

CREATE TABLE quote_line_item (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id          uuid NOT NULL REFERENCES organization (id),
    quote_version_id         uuid NOT NULL REFERENCES quote_version (id),
    material_id              uuid REFERENCES material (id),
    catalog_item_id          uuid REFERENCES catalog_item (id),
    descricao                text NOT NULL,
    quantidade               numeric(12,4) NOT NULL CHECK (quantidade > 0),
    unidade                  unidade_medida NOT NULL,
    preco_unitario_snapshot  numeric(12,2) NOT NULL CHECK (preco_unitario_snapshot >= 0),
    subtotal                 numeric(14,2) NOT NULL CHECK (subtotal >= 0),
    CONSTRAINT ck_quote_line_item_origem CHECK (material_id IS NOT NULL OR catalog_item_id IS NOT NULL)
);

CREATE INDEX idx_quote_line_item_org ON quote_line_item (organization_id);
CREATE INDEX idx_quote_line_item_version ON quote_line_item (quote_version_id);

-- Linha snapshotada pertence a uma versão; some a versão vira imutável, a linha segue junto.
CREATE OR REPLACE FUNCTION fn_quote_line_item_bloquear_se_versao_aprovada() RETURNS trigger AS $$
DECLARE
    v_status quote_version_status;
BEGIN
    SELECT status INTO v_status FROM quote_version WHERE id = OLD.quote_version_id;
    IF v_status = 'APROVADO' THEN
        RAISE EXCEPTION 'quote_line_item de versão já aprovada é imutável';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_quote_line_item_imutavel_aprovado
    BEFORE UPDATE OR DELETE ON quote_line_item
    FOR EACH ROW EXECUTE FUNCTION fn_quote_line_item_bloquear_se_versao_aprovada();
