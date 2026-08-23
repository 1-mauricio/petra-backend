package com.marmorarias.measurement.application;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Parâmetros comerciais (margem/desconto/mão de obra) não são persistidos no orçamento (schema
 * V1-V13 não tem colunas para isso — só o valor_total final), então precisam ser informados de
 * novo aqui para o recálculo reproduzir a mesma fórmula usada na criação do orçamento.
 */
public record AprovarMedicaoRequest(Map<UUID, BigDecimal> fatorPerdaPorMaterial, BigDecimal fatorPerdaDefault,
                                     BigDecimal margem, BigDecimal desconto, UUID maoDeObraCatalogItemId,
                                     BigDecimal maoDeObraHoras) {
}
