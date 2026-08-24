package com.marmorarias.measurement.adapter.persistence;

import com.marmorarias.measurement.domain.MeasurementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeasurementListItem(
        UUID id,
        UUID orderId,
        MeasurementStatus status,
        Instant dataAgendada,
        Instant dataMedicao,
        UUID tecnicoResponsavel,
        Instant approvedAt,
        List<Peca> pieces) {

    public record Peca(
            UUID id,
            String descricao,
            BigDecimal larguraM,
            BigDecimal comprimentoM,
            int quantidade,
            BigDecimal areaM2) {
    }
}
