package com.marmorarias.identity.domain;

/**
 * Espelha o enum user_role do Postgres (V1). Constantes em minúsculo de propósito: o
 * claim "role" do JWT do Supabase e o enum nativo do banco já usam esses valores, então
 * Role.valueOf(claim) mapeia direto, sem tabela de conversão.
 */
public enum Role {
    admin,
    comercial,
    producao
}
