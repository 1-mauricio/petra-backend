package com.marmorarias.measurement.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Payload do medicao-pwa: dimensões em CENTÍMETROS (o app de campo não trabalha em metros). */
public record RegistrarMedicaoCampoRequest(String localId, List<PecaCampoRequest> pecas, Instant criadoEm) {

    public record PecaCampoRequest(String id, String descricao, BigDecimal largura, BigDecimal altura,
                                    BigDecimal espessura, String observacao) {
    }
}
