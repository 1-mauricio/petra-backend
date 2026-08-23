package com.marmorarias.quoting.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de cálculo de orçamento. Função pura: recebe valores já congelados (catálogo snapshot) e
 * não faz I/O. Roda tanto no orçamento (medidas estimadas) quanto pós-medição (medidas reais) — a
 * divergência é comparada fora daqui, entre duas chamadas desta função.
 */
public final class OrcamentoCalculator {

  private static final int CALC_SCALE = 6;
  private static final int MONEY_SCALE = 2;
  private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  private OrcamentoCalculator() {}

  public record Acabamento(String catalogItemId, BigDecimal metrosLineares) {}

  public record Recorte(String catalogItemId, BigDecimal quantidade) {}

  public record Peca(
      String materialId,
      BigDecimal largura,
      BigDecimal comprimento,
      List<Acabamento> acabamentos,
      List<Recorte> recortes) {
    BigDecimal area() {
      return largura.multiply(comprimento);
    }
  }

  public record MaterialSnapshot(
      BigDecimal precoM2, BigDecimal chapaLargura, BigDecimal chapaComprimento) {
    BigDecimal areaChapa() {
      return chapaLargura.multiply(chapaComprimento);
    }
  }

  public record CatalogItemSnapshot(BigDecimal preco) {}

  public record CatalogoSnapshot(
      Map<String, MaterialSnapshot> materiais, Map<String, CatalogItemSnapshot> catalogItems) {
    MaterialSnapshot material(String materialId) {
      MaterialSnapshot m = materiais.get(materialId);
      if (m == null) {
        throw new IllegalArgumentException("Material não encontrado no catálogo: " + materialId);
      }
      return m;
    }

    CatalogItemSnapshot item(String catalogItemId) {
      CatalogItemSnapshot i = catalogItems.get(catalogItemId);
      if (i == null) {
        throw new IllegalArgumentException("Item de catálogo não encontrado: " + catalogItemId);
      }
      return i;
    }
  }

  /**
   * fatorPerdaPorTipoPeca é chaveado por materialId — a entrada não distingue um "tipo de peça"
   * separado do material, então o tipo é o próprio material. fatorPerdaDefault cobre materiais sem
   * entrada explícita no mapa.
   */
  public record OrcamentoParams(
      Map<String, BigDecimal> fatorPerdaPorTipoPeca,
      BigDecimal fatorPerdaDefault,
      BigDecimal margem,
      BigDecimal desconto,
      BigDecimal maoDeObra) {
    BigDecimal fatorPerda(String materialId) {
      return fatorPerdaPorTipoPeca.getOrDefault(materialId, fatorPerdaDefault);
    }
  }

  public record BreakdownItem(String descricao, BigDecimal valor) {}

  public record OrcamentoResultado(
      BigDecimal custoInterno, BigDecimal precoCliente, List<BreakdownItem> breakdown) {}

  public static OrcamentoResultado calcularOrcamento(
      List<Peca> pecas, CatalogoSnapshot catalogo, OrcamentoParams params) {
    List<BreakdownItem> breakdown = new ArrayList<>();
    BigDecimal subtotalCliente = BigDecimal.ZERO;
    Map<String, BigDecimal> areaComPerdaPorMaterial = new LinkedHashMap<>();

    for (int i = 0; i < pecas.size(); i++) {
      Peca peca = pecas.get(i);
      MaterialSnapshot material = catalogo.material(peca.materialId());
      BigDecimal fatorPerda = params.fatorPerda(peca.materialId());
      BigDecimal areaComPerda = peca.area().multiply(BigDecimal.ONE.add(fatorPerda));

      areaComPerdaPorMaterial.merge(peca.materialId(), areaComPerda, BigDecimal::add);

      BigDecimal custoPeca = areaComPerda.multiply(material.precoM2());
      subtotalCliente = subtotalCliente.add(custoPeca);
      breakdown.add(
          new BreakdownItem("peça " + i + " (" + peca.materialId() + ")", round(custoPeca)));

      for (Acabamento acabamento : peca.acabamentos()) {
        BigDecimal preco = catalogo.item(acabamento.catalogItemId()).preco();
        BigDecimal custoAcabamento = acabamento.metrosLineares().multiply(preco);
        subtotalCliente = subtotalCliente.add(custoAcabamento);
        breakdown.add(
            new BreakdownItem("acabamento " + acabamento.catalogItemId(), round(custoAcabamento)));
      }

      for (Recorte recorte : peca.recortes()) {
        BigDecimal preco = catalogo.item(recorte.catalogItemId()).preco();
        BigDecimal custoRecorte = recorte.quantidade().multiply(preco);
        subtotalCliente = subtotalCliente.add(custoRecorte);
        breakdown.add(new BreakdownItem("recorte " + recorte.catalogItemId(), round(custoRecorte)));
      }
    }

    subtotalCliente = subtotalCliente.add(params.maoDeObra());
    breakdown.add(new BreakdownItem("mão de obra/instalação", round(params.maoDeObra())));

    BigDecimal comMargem = subtotalCliente.multiply(BigDecimal.ONE.add(params.margem()));
    breakdown.add(new BreakdownItem("margem", round(comMargem.subtract(subtotalCliente))));

    BigDecimal comDesconto = comMargem.multiply(BigDecimal.ONE.subtract(params.desconto()));
    breakdown.add(new BreakdownItem("desconto", round(comDesconto.subtract(comMargem))));

    BigDecimal precoCliente = round(comDesconto);

    BigDecimal custoInterno = BigDecimal.ZERO;
    for (Map.Entry<String, BigDecimal> entry : areaComPerdaPorMaterial.entrySet()) {
      MaterialSnapshot material = catalogo.material(entry.getKey());
      BigDecimal chapas = estimarChapasConsumidas(entry.getValue(), material.areaChapa());
      custoInterno =
          custoInterno.add(chapas.multiply(material.areaChapa()).multiply(material.precoM2()));
    }
    custoInterno = round(custoInterno);

    return new OrcamentoResultado(custoInterno, precoCliente, breakdown);
  }

  /**
   * Estimativa de chapas por fator de perda, sem nesting real (MVP). Isolado para ser substituído
   * por nesting real na Fase 2.
   */
  static BigDecimal estimarChapasConsumidas(BigDecimal areaComPerda, BigDecimal areaChapa) {
    return areaComPerda.divide(areaChapa, CALC_SCALE, ROUNDING).setScale(0, RoundingMode.CEILING);
  }

  private static BigDecimal round(BigDecimal valor) {
    return valor.setScale(MONEY_SCALE, ROUNDING);
  }
}
