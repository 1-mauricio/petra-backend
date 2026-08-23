package com.marmorarias.measurement.application;

import java.math.BigDecimal;
import java.util.UUID;

public record PecaMedidaRequest(UUID materialId, String descricao, BigDecimal larguraM, BigDecimal comprimentoM,
                                 int quantidade, BigDecimal fatorPerdaAplicado) {
}
