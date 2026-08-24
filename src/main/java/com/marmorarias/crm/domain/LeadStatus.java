package com.marmorarias.crm.domain;

/** Espelha o enum lead_status do Postgres (V1, migrado em V22). */
public enum LeadStatus {
    NOVO,
    CONTATADO,
    QUALIFICADO,
    CONVERTIDO,
    PERDIDO
}
