CREATE TABLE measurement (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     uuid NOT NULL REFERENCES organization (id),
    order_id            uuid NOT NULL REFERENCES customer_order (id),
    status              measurement_status NOT NULL DEFAULT 'PENDENTE',
    data_medicao        timestamptz NOT NULL DEFAULT now(),
    tecnico_responsavel uuid REFERENCES user_profile (id),
    approved_at         timestamptz,
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_measurement_org ON measurement (organization_id);
CREATE INDEX idx_measurement_order ON measurement (order_id);

-- Measurement é imutável após APROVADO; nova medição = novo registro.
CREATE OR REPLACE FUNCTION fn_measurement_bloquear_update_aprovado() RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'APROVADO' THEN
        RAISE EXCEPTION 'measurement % já aprovado é imutável', OLD.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_measurement_imutavel_aprovado
    BEFORE UPDATE ON measurement
    FOR EACH ROW EXECUTE FUNCTION fn_measurement_bloquear_update_aprovado();

CREATE TABLE measurement_piece (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id         uuid NOT NULL REFERENCES organization (id),
    measurement_id          uuid NOT NULL REFERENCES measurement (id),
    descricao               text NOT NULL,
    largura_m               numeric(10,3) NOT NULL CHECK (largura_m > 0),
    comprimento_m           numeric(10,3) NOT NULL CHECK (comprimento_m > 0),
    quantidade              int NOT NULL DEFAULT 1 CHECK (quantidade > 0),
    area_m2                 numeric(12,4) GENERATED ALWAYS AS (largura_m * comprimento_m * quantidade) STORED,
    fator_perda_aplicado    numeric(5,4)
);

CREATE INDEX idx_measurement_piece_org ON measurement_piece (organization_id);
CREATE INDEX idx_measurement_piece_measurement ON measurement_piece (measurement_id);

-- Peça é imutável se a medição-pai já foi APROVADO.
CREATE OR REPLACE FUNCTION fn_measurement_piece_bloquear_se_pai_aprovado() RETURNS trigger AS $$
DECLARE
    v_status measurement_status;
BEGIN
    SELECT status INTO v_status FROM measurement WHERE id = OLD.measurement_id;
    IF v_status = 'APROVADO' THEN
        RAISE EXCEPTION 'measurement_piece de medição já aprovada é imutável';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_measurement_piece_imutavel_aprovado
    BEFORE UPDATE OR DELETE ON measurement_piece
    FOR EACH ROW EXECUTE FUNCTION fn_measurement_piece_bloquear_se_pai_aprovado();
