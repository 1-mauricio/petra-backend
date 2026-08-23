package com.marmorarias.measurement.domain;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Invariante 2: divergência medição x orçamento aprovado que dispara REVISAO_ORCAMENTO —
 * mais de 5% OU mais de R$500 (o que ocorrer primeiro), ambos configuráveis por organização.
 * Função pura, sem I/O, para ser testada isoladamente do resto do fluxo de aprovação.
 */
public final class DivergenceChecker {

    private DivergenceChecker() {
    }

    public record Resultado(BigDecimal divergenciaAbs, BigDecimal divergenciaPerc, boolean excedeTolerancia) {
    }

    public static Resultado avaliar(BigDecimal valorAprovado, BigDecimal valorRecalculado, BigDecimal toleranciaPerc,
                                     BigDecimal toleranciaAbs) {
        BigDecimal divergenciaAbs = valorRecalculado.subtract(valorAprovado).abs();
        BigDecimal divergenciaPerc = valorAprovado.signum() == 0
                ? BigDecimal.ZERO
                : divergenciaAbs.divide(valorAprovado, MathContext.DECIMAL64).multiply(BigDecimal.valueOf(100));

        boolean excede = divergenciaPerc.compareTo(toleranciaPerc) > 0 || divergenciaAbs.compareTo(toleranciaAbs) > 0;
        return new Resultado(divergenciaAbs, divergenciaPerc, excede);
    }
}
