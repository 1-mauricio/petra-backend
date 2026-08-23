package com.marmorarias.quoting.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CriarOrcamentoRequest(UUID customerId, UUID leadId, List<PecaRequest> pecas,
                                     Map<UUID, BigDecimal> fatorPerdaPorMaterial, BigDecimal fatorPerdaDefault,
                                     BigDecimal margem, BigDecimal desconto, UUID maoDeObraCatalogItemId,
                                     BigDecimal maoDeObraHoras) {
}
