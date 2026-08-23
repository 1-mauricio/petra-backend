package com.marmorarias.production.domain;

/** Espelha o enum production_task_status do Postgres (V1). MVP: só o status muda. */
public enum ProductionTaskStatus {
    PENDENTE,
    EM_ANDAMENTO,
    CONCLUIDA
}
