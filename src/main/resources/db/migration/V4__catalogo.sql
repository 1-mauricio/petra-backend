CREATE TABLE material (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     uuid NOT NULL REFERENCES organization (id),
    tipo                text NOT NULL,
    cor                 text NOT NULL,
    preco_m2            numeric(12,2) NOT NULL CHECK (preco_m2 >= 0),
    largura_chapa       numeric(10,3) NOT NULL CHECK (largura_chapa > 0),
    comprimento_chapa   numeric(10,3) NOT NULL CHECK (comprimento_chapa > 0),
    ativo               boolean NOT NULL DEFAULT true,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_material_org ON material (organization_id);

CREATE TRIGGER trg_material_updated_at
    BEFORE UPDATE ON material
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TABLE catalog_item (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization (id),
    tipo            catalog_item_tipo NOT NULL,
    descricao       text NOT NULL,
    unidade         unidade_medida NOT NULL,
    preco           numeric(12,2) NOT NULL CHECK (preco >= 0),
    ativo           boolean NOT NULL DEFAULT true,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_catalog_item_org ON catalog_item (organization_id);

CREATE TRIGGER trg_catalog_item_updated_at
    BEFORE UPDATE ON catalog_item
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
