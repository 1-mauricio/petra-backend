-- Invariante: desconto acima deste limite (percentual do preço ao cliente) só pode ser
-- concedido por um admin — aplicado em QuoteService, não no banco (é regra de autorização,
-- não de integridade de dados).
ALTER TABLE org_settings ADD COLUMN desconto_limite_perc numeric(5,2) NOT NULL DEFAULT 20.00;
