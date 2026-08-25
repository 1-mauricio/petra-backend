-- V5/V7 bloqueavam UPDATE de quote_version/measurement já APROVADO, mas não DELETE — app_user
-- tem GRANT DELETE (V11) nessas tabelas. Um registro aprovado sem filhos (line_item/piece já
-- deletados, ou nunca criados) podia ser apagado direto. Estende os triggers pra também cobrir
-- DELETE, mesmo padrão que quote_line_item/measurement_piece já usam desde V5/V7.

CREATE OR REPLACE FUNCTION fn_quote_version_bloquear_update_aprovado() RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'APROVADO' THEN
        RAISE EXCEPTION 'quote_version % já aprovada é imutável', OLD.id;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER trg_quote_version_imutavel_aprovado ON quote_version;
CREATE TRIGGER trg_quote_version_imutavel_aprovado
    BEFORE UPDATE OR DELETE ON quote_version
    FOR EACH ROW EXECUTE FUNCTION fn_quote_version_bloquear_update_aprovado();

CREATE OR REPLACE FUNCTION fn_measurement_bloquear_update_aprovado() RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'APROVADO' THEN
        RAISE EXCEPTION 'measurement % já aprovado é imutável', OLD.id;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER trg_measurement_imutavel_aprovado ON measurement;
CREATE TRIGGER trg_measurement_imutavel_aprovado
    BEFORE UPDATE OR DELETE ON measurement
    FOR EACH ROW EXECUTE FUNCTION fn_measurement_bloquear_update_aprovado();

-- Self-check: DELETE de um aprovado deve falhar; DELETE de um não-aprovado deve funcionar.
-- Autocontido (cria sua própria organização/customer/quote) — não depende dos seeds de
-- V12/V17, que não rodam em produção. A linha APROVADO é, por design, impossível de limpar
-- com DELETE/UPDATE depois de criada — em vez de tentar, o bloco força uma exceção sentinela
-- no fim para desfazer via savepoint todo o INSERT de teste (incluindo a linha imutável) de
-- uma vez, sem deixar resíduo.
DO $$
DECLARE
    v_org uuid;
    v_customer uuid;
    v_quote uuid;
    v_qv_aprovado uuid;
    v_qv_rascunho uuid;
    v_delete_bloqueado boolean := false;
BEGIN
    BEGIN
        INSERT INTO organization (nome) VALUES ('__selfcheck_v29__')
        RETURNING id INTO v_org;

        PERFORM set_config('app.current_org_id', v_org::text, true);

        INSERT INTO customer (organization_id, tipo, nome, cpf_cnpj)
        VALUES (v_org, 'PF', '__selfcheck_v29__', '12345678909')
        RETURNING id INTO v_customer;

        INSERT INTO quote (organization_id, customer_id) VALUES (v_org, v_customer)
        RETURNING id INTO v_quote;

        INSERT INTO quote_version (organization_id, quote_id, version_number, status, valor_total)
        VALUES (v_org, v_quote, 1, 'APROVADO', 100)
        RETURNING id INTO v_qv_aprovado;

        INSERT INTO quote_version (organization_id, quote_id, version_number, status, valor_total)
        VALUES (v_org, v_quote, 2, 'RASCUNHO', 50)
        RETURNING id INTO v_qv_rascunho;

        BEGIN
            DELETE FROM quote_version WHERE id = v_qv_aprovado;
        EXCEPTION WHEN OTHERS THEN
            v_delete_bloqueado := true;
        END;
        IF NOT v_delete_bloqueado THEN
            RAISE EXCEPTION 'self-check falhou: DELETE de quote_version APROVADO deveria ter sido bloqueado';
        END IF;

        DELETE FROM quote_version WHERE id = v_qv_rascunho;
        IF EXISTS (SELECT 1 FROM quote_version WHERE id = v_qv_rascunho) THEN
            RAISE EXCEPTION 'self-check falhou: DELETE de quote_version RASCUNHO deveria ter funcionado';
        END IF;

        RAISE EXCEPTION 'ponytail_selfcheck_v29_ok';
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLERRM <> 'ponytail_selfcheck_v29_ok' THEN
                RAISE;
            END IF;
    END;
END;
$$;
