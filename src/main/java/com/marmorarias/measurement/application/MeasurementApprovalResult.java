package com.marmorarias.measurement.application;

import com.marmorarias.measurement.adapter.persistence.MeasurementEntity;
import com.marmorarias.measurement.adapter.persistence.MeasurementPieceEntity;
import com.marmorarias.measurement.domain.DivergenceChecker;
import java.math.BigDecimal;
import java.util.List;

public record MeasurementApprovalResult(MeasurementEntity measurement, List<MeasurementPieceEntity> pecas,
                                         BigDecimal valorRecalculado, DivergenceChecker.Resultado divergencia) {

    public boolean exigeRevisaoOrcamento() {
        return divergencia.excedeTolerancia();
    }
}
