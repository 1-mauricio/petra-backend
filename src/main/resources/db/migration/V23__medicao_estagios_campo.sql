-- Estágios de campo do levantamento técnico: agendado -> em campo -> concluído -> aprovado/rejeitado.
-- PENDENTE (medição já com peças, aguardando aprovação) mapeia para CONCLUIDO.
ALTER TYPE measurement_status RENAME TO measurement_status_old;
CREATE TYPE measurement_status AS ENUM ('AGENDADO', 'EM_CAMPO', 'CONCLUIDO', 'APROVADO', 'REJEITADO');

ALTER TABLE measurement ALTER COLUMN status DROP DEFAULT;
ALTER TABLE measurement ALTER COLUMN status TYPE measurement_status USING (
    CASE status::text
        WHEN 'PENDENTE' THEN 'CONCLUIDO'
        ELSE status::text
    END
)::measurement_status;
ALTER TABLE measurement ALTER COLUMN status SET DEFAULT 'AGENDADO';

DROP TYPE measurement_status_old;

ALTER TABLE measurement ADD COLUMN data_agendada timestamptz;
