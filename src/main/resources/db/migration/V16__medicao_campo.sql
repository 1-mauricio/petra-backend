-- Captura de campo (app medicao-pwa): o técnico mede largura/altura/espessura na hora, sem
-- escolher material — isso é feito depois, no escritório, antes da aprovação (invariante 2
-- precisa de material_id pra recalcular, mas não no momento do registro). Migration aditiva.
ALTER TABLE measurement_piece ALTER COLUMN material_id DROP NOT NULL;
ALTER TABLE measurement_piece ADD COLUMN espessura_m numeric(10,3);
ALTER TABLE measurement_piece ADD COLUMN observacao text;
