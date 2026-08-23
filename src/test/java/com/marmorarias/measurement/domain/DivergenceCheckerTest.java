package com.marmorarias.measurement.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DivergenceCheckerTest {

    private static final BigDecimal TOLERANCIA_PERC = new BigDecimal("5");
    private static final BigDecimal TOLERANCIA_ABS = new BigDecimal("500");

    @Test
    void dentroDeAmbasAsToleranciasNaoExcede() {
        // 1000 -> 1030: 3% e R$30 de diferença, abaixo dos dois limites
        var resultado = DivergenceChecker.avaliar(new BigDecimal("1000"), new BigDecimal("1030"), TOLERANCIA_PERC,
                TOLERANCIA_ABS);
        assertFalse(resultado.excedeTolerancia());
    }

    @Test
    void excedePercentualDisparaMesmoComDiferencaAbsBaixa() {
        // 100 -> 110: 10% de diferença (> 5%), mas só R$10 (< R$500)
        var resultado = DivergenceChecker.avaliar(new BigDecimal("100"), new BigDecimal("110"), TOLERANCIA_PERC,
                TOLERANCIA_ABS);
        assertTrue(resultado.excedeTolerancia());
    }

    @Test
    void excedeValorAbsolutoDisparaMesmoComPercentualBaixo() {
        // 100000 -> 100600: 0.6% (< 5%), mas R$600 (> R$500)
        var resultado = DivergenceChecker.avaliar(new BigDecimal("100000"), new BigDecimal("100600"), TOLERANCIA_PERC,
                TOLERANCIA_ABS);
        assertTrue(resultado.excedeTolerancia());
    }

    @Test
    void valorMenorQueOAprovadoTambemConta() {
        var resultado = DivergenceChecker.avaliar(new BigDecimal("1000"), new BigDecimal("400"), TOLERANCIA_PERC,
                TOLERANCIA_ABS);
        assertTrue(resultado.excedeTolerancia());
    }
}
