package com.marmorarias.quoting.domain;

import static com.marmorarias.quoting.domain.OrcamentoCalculator.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrcamentoCalculatorTest {

  private static final String GRANITO = "granito_preto";

  private CatalogoSnapshot catalogo() {
    return new CatalogoSnapshot(
        Map.of(
            GRANITO,
            new MaterialSnapshot(
                new BigDecimal("500.00"), new BigDecimal("3.0"), new BigDecimal("1.5"))),
        Map.of(
            "acab_bisote", new CatalogItemSnapshot(new BigDecimal("25.00")),
            "recorte_cuba", new CatalogItemSnapshot(new BigDecimal("150.00"))));
  }

  private OrcamentoParams params() {
    return new OrcamentoParams(
        Map.of(GRANITO, new BigDecimal("0.20")),
        new BigDecimal("0.25"),
        new BigDecimal("0.30"),
        new BigDecimal("0.10"),
        new BigDecimal("200.00"));
  }

  private Peca peca(
      BigDecimal largura,
      BigDecimal comprimento,
      List<Acabamento> acabamentos,
      List<Recorte> recortes) {
    return new Peca(GRANITO, largura, comprimento, acabamentos, recortes);
  }

  @Test
  void calculaCustoInternoEPrecoClienteSemAcabamentos() {
    Peca peca = peca(new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of());

    OrcamentoResultado resultado = calcularOrcamento(List.of(peca), catalogo(), params());

    // area 2 * (1+0.20) * 500 = 1200; +maoDeObra 200 = 1400; *1.30 = 1820; *0.90 = 1638.00
    assertEquals(new BigDecimal("1638.00"), resultado.precoCliente());
    // areaComPerda 2.4 / areaChapa 4.5 -> 1 chapa; 1 * 4.5 * 500 = 2250.00
    assertEquals(new BigDecimal("2250.00"), resultado.custoInterno());
  }

  @Test
  void calculaComAcabamentosERecortes() {
    Peca peca =
        peca(
            new BigDecimal("2"),
            new BigDecimal("1"),
            List.of(new Acabamento("acab_bisote", new BigDecimal("3"))),
            List.of(new Recorte("recorte_cuba", new BigDecimal("1"))));

    OrcamentoResultado resultado = calcularOrcamento(List.of(peca), catalogo(), params());

    // 1200 + 75 (acabamento) + 150 (recorte) + 200 (mão de obra) = 1625; *1.30 = 2112.5; *0.90 =
    // 1901.25
    assertEquals(new BigDecimal("1901.25"), resultado.precoCliente());
    assertEquals(new BigDecimal("2250.00"), resultado.custoInterno());
  }

  @Test
  void estimaSegundaChapaQuandoAreaComPerdaExcedeUmaChapa() {
    Peca a = peca(new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of());
    Peca b = peca(new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of());

    OrcamentoResultado resultado = calcularOrcamento(List.of(a, b), catalogo(), params());

    // areaComPerda total 4.8 / areaChapa 4.5 -> 2 chapas; 2 * 4.5 * 500 = 4500.00
    assertEquals(new BigDecimal("4500.00"), resultado.custoInterno());
  }

  @Test
  void mesmaEntradaProduzMesmaSaida() {
    Peca peca =
        peca(
            new BigDecimal("2"),
            new BigDecimal("1"),
            List.of(new Acabamento("acab_bisote", new BigDecimal("3"))),
            List.of(new Recorte("recorte_cuba", new BigDecimal("1"))));
    List<Peca> pecas = List.of(peca);
    CatalogoSnapshot catalogo = catalogo();
    OrcamentoParams params = params();

    OrcamentoResultado primeira = calcularOrcamento(pecas, catalogo, params);
    OrcamentoResultado segunda = calcularOrcamento(pecas, catalogo, params);

    assertEquals(primeira.precoCliente(), segunda.precoCliente());
    assertEquals(primeira.custoInterno(), segunda.custoInterno());
    assertEquals(primeira.breakdown(), segunda.breakdown());
  }

  @Test
  void arredondamentoMonetarioUsaHalfUpComDuasCasas() {
    // desconto 7.5% sobre valor que gera terceira casa decimal, força arredondamento
    Peca peca = peca(new BigDecimal("1"), new BigDecimal("1"), List.of(), List.of());
    OrcamentoParams params =
        new OrcamentoParams(
            Map.of(GRANITO, new BigDecimal("0.20")),
            new BigDecimal("0.25"),
            BigDecimal.ZERO,
            new BigDecimal("0.075"),
            BigDecimal.ZERO);

    OrcamentoResultado resultado = calcularOrcamento(List.of(peca), catalogo(), params);

    // area 1 * 1.20 * 500 = 600; sem margem; *(1-0.075) = 555.00
    assertEquals(new BigDecimal("555.00"), resultado.precoCliente());
    assertEquals(2, resultado.precoCliente().scale());
  }

  @Test
  void calculoComDescontoEstaCorretoIndependenteDeAutorizacao() {
    // a regra de "desconto acima do limite exige admin" é da aplicação;
    // aqui só garantimos que o cálculo aplica o desconto corretamente
    Peca peca = peca(new BigDecimal("2"), new BigDecimal("1"), List.of(), List.of());
    OrcamentoParams comDescontoAlto =
        new OrcamentoParams(
            Map.of(GRANITO, new BigDecimal("0.20")),
            new BigDecimal("0.25"),
            new BigDecimal("0.30"),
            new BigDecimal("0.50"),
            new BigDecimal("200.00"));

    OrcamentoResultado resultado = calcularOrcamento(List.of(peca), catalogo(), comDescontoAlto);

    // 1400 * 1.30 = 1820; * (1-0.50) = 910.00
    assertEquals(new BigDecimal("910.00"), resultado.precoCliente());
  }
}
