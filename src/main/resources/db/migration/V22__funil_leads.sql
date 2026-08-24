-- Funil de leads completo: novo -> contatado -> qualificado -> convertido/perdido.
-- ABERTO/EM_NEGOCIACAO/GANHO mapeiam para NOVO/CONTATADO/CONVERTIDO; PERDIDO é preservado.
ALTER TYPE lead_status RENAME TO lead_status_old;
CREATE TYPE lead_status AS ENUM ('NOVO', 'CONTATADO', 'QUALIFICADO', 'CONVERTIDO', 'PERDIDO');

ALTER TABLE lead DROP CONSTRAINT ck_lead_motivo_perda;
ALTER TABLE lead ALTER COLUMN status DROP DEFAULT;
ALTER TABLE lead ALTER COLUMN status TYPE lead_status USING (
    CASE status::text
        WHEN 'ABERTO' THEN 'NOVO'
        WHEN 'EM_NEGOCIACAO' THEN 'CONTATADO'
        WHEN 'GANHO' THEN 'CONVERTIDO'
        ELSE status::text
    END
)::lead_status;
ALTER TABLE lead ALTER COLUMN status SET DEFAULT 'NOVO';
ALTER TABLE lead ADD CONSTRAINT ck_lead_motivo_perda CHECK (status <> 'PERDIDO' OR motivo_perda IS NOT NULL);

DROP TYPE lead_status_old;
