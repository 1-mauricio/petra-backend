-- Invariante 2 exige recalcular o orçamento com as medidas reais via o mesmo motor de
-- cálculo (V7 registrou dimensões da peça, mas não o material) — sem material_id não há
-- preço para recalcular. Migration aditiva, não mexe em V1-V13.
ALTER TABLE measurement_piece ADD COLUMN material_id uuid NOT NULL REFERENCES material (id);
