package com.marmorarias.quoting.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PecaRequest(UUID materialId, BigDecimal largura, BigDecimal comprimento,
                           List<AcabamentoRequest> acabamentos, List<RecorteRequest> recortes) {

    public record AcabamentoRequest(UUID catalogItemId, BigDecimal metrosLineares) {
    }

    public record RecorteRequest(UUID catalogItemId, BigDecimal quantidade) {
    }
}
