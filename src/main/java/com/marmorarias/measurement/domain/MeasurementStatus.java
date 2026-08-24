package com.marmorarias.measurement.domain;

/** Espelha o enum measurement_status do Postgres (V1, migrado em V23). */
public enum MeasurementStatus {
    AGENDADO,
    EM_CAMPO,
    CONCLUIDO,
    APROVADO,
    REJEITADO
}
