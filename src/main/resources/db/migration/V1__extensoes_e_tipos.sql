-- Extensões
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Enums de domínio (convenção: tipo Postgres nativo para todo conjunto fechado de estados)
CREATE TYPE user_role AS ENUM ('admin', 'comercial', 'producao');

CREATE TYPE customer_tipo AS ENUM ('PF', 'PJ');

CREATE TYPE lead_status AS ENUM ('ABERTO', 'EM_NEGOCIACAO', 'GANHO', 'PERDIDO');

CREATE TYPE catalog_item_tipo AS ENUM ('ACABAMENTO', 'RECORTE', 'MAO_DE_OBRA');

CREATE TYPE unidade_medida AS ENUM ('METRO_LINEAR', 'METRO_QUADRADO', 'UNIDADE', 'HORA');

CREATE TYPE quote_version_status AS ENUM ('RASCUNHO', 'ENVIADO', 'APROVADO', 'REJEITADO');

CREATE TYPE order_state AS ENUM (
    'ORCAMENTO',
    'APROVACAO',
    'PEDIDO',
    'LEVANTAMENTO_TECNICO',
    'REVISAO_ORCAMENTO',
    'PRODUCAO',
    'ENTREGA',
    'INSTALACAO',
    'CONCLUIDO',
    'CANCELADO'
);

CREATE TYPE measurement_status AS ENUM ('PENDENTE', 'APROVADO', 'REJEITADO');

CREATE TYPE production_task_status AS ENUM ('PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA');

CREATE TYPE delivery_status AS ENUM ('AGENDADA', 'EM_ROTA', 'ENTREGUE', 'CANCELADA');

CREATE TYPE installment_status AS ENUM ('PENDENTE', 'PAGO', 'ATRASADO', 'CANCELADO');

-- Validação de CPF (dígitos verificadores, módulo 11)
CREATE OR REPLACE FUNCTION fn_validar_cpf(p_cpf text) RETURNS boolean AS $$
DECLARE
    v_cpf text := regexp_replace(coalesce(p_cpf, ''), '\D', '', 'g');
    v_soma int;
    v_resto int;
    i int;
BEGIN
    IF length(v_cpf) <> 11 OR v_cpf ~ '^(\d)\1{10}$' THEN
        RETURN false;
    END IF;

    v_soma := 0;
    FOR i IN 1..9 LOOP
        v_soma := v_soma + substring(v_cpf FROM i FOR 1)::int * (11 - i);
    END LOOP;
    v_resto := (v_soma * 10) % 11;
    IF v_resto = 10 THEN v_resto := 0; END IF;
    IF v_resto <> substring(v_cpf FROM 10 FOR 1)::int THEN RETURN false; END IF;

    v_soma := 0;
    FOR i IN 1..10 LOOP
        v_soma := v_soma + substring(v_cpf FROM i FOR 1)::int * (12 - i);
    END LOOP;
    v_resto := (v_soma * 10) % 11;
    IF v_resto = 10 THEN v_resto := 0; END IF;
    IF v_resto <> substring(v_cpf FROM 11 FOR 1)::int THEN RETURN false; END IF;

    RETURN true;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Validação de CNPJ (dígitos verificadores, módulo 11)
CREATE OR REPLACE FUNCTION fn_validar_cnpj(p_cnpj text) RETURNS boolean AS $$
DECLARE
    v_cnpj text := regexp_replace(coalesce(p_cnpj, ''), '\D', '', 'g');
    v_pesos1 int[] := ARRAY[5,4,3,2,9,8,7,6,5,4,3,2];
    v_pesos2 int[] := ARRAY[6,5,4,3,2,9,8,7,6,5,4,3,2];
    v_soma int;
    v_resto int;
    i int;
BEGIN
    IF length(v_cnpj) <> 14 OR v_cnpj ~ '^(\d)\1{13}$' THEN
        RETURN false;
    END IF;

    v_soma := 0;
    FOR i IN 1..12 LOOP
        v_soma := v_soma + substring(v_cnpj FROM i FOR 1)::int * v_pesos1[i];
    END LOOP;
    v_resto := v_soma % 11;
    v_resto := CASE WHEN v_resto < 2 THEN 0 ELSE 11 - v_resto END;
    IF v_resto <> substring(v_cnpj FROM 13 FOR 1)::int THEN RETURN false; END IF;

    v_soma := 0;
    FOR i IN 1..13 LOOP
        v_soma := v_soma + substring(v_cnpj FROM i FOR 1)::int * v_pesos2[i];
    END LOOP;
    v_resto := v_soma % 11;
    v_resto := CASE WHEN v_resto < 2 THEN 0 ELSE 11 - v_resto END;
    IF v_resto <> substring(v_cnpj FROM 14 FOR 1)::int THEN RETURN false; END IF;

    RETURN true;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION fn_validar_cpf_cnpj(p_tipo customer_tipo, p_valor text) RETURNS boolean AS $$
BEGIN
    RETURN CASE p_tipo
        WHEN 'PF' THEN fn_validar_cpf(p_valor)
        WHEN 'PJ' THEN fn_validar_cnpj(p_valor)
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Utilitário genérico para colunas updated_at
CREATE OR REPLACE FUNCTION fn_set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
