package com.marmorarias.measurement.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "measurement_piece")
public class MeasurementPieceEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "measurement_id", nullable = false)
    private UUID measurementId;

    @Column(name = "material_id")
    private UUID materialId;

    private String descricao;

    @Column(name = "largura_m", nullable = false)
    private BigDecimal larguraM;

    @Column(name = "comprimento_m", nullable = false)
    private BigDecimal comprimentoM;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "fator_perda_aplicado")
    private BigDecimal fatorPerdaAplicado;

    @Column(name = "area_m2", insertable = false, updatable = false)
    private BigDecimal areaM2;

    @Column(name = "espessura_m")
    private BigDecimal espessuraM;

    private String observacao;

    protected MeasurementPieceEntity() {
    }

    public MeasurementPieceEntity(UUID organizationId, UUID measurementId, UUID materialId, String descricao,
                                   BigDecimal larguraM, BigDecimal comprimentoM, int quantidade,
                                   BigDecimal fatorPerdaAplicado) {
        this.organizationId = organizationId;
        this.measurementId = measurementId;
        this.materialId = materialId;
        this.descricao = descricao;
        this.larguraM = larguraM;
        this.comprimentoM = comprimentoM;
        this.quantidade = quantidade;
        this.fatorPerdaAplicado = fatorPerdaAplicado;
    }

    /** Captura de campo (medicao-pwa): sem material nem quantidade, com espessura e observação livres. */
    public MeasurementPieceEntity(UUID organizationId, UUID measurementId, String descricao, BigDecimal larguraM,
                                   BigDecimal comprimentoM, BigDecimal espessuraM, String observacao) {
        this.organizationId = organizationId;
        this.measurementId = measurementId;
        this.descricao = descricao;
        this.larguraM = larguraM;
        this.comprimentoM = comprimentoM;
        this.quantidade = 1;
        this.espessuraM = espessuraM;
        this.observacao = observacao;
    }

    /** Atribuição do material fica pra depois (escritório, antes da aprovação — ver V16). */
    public void atribuirMaterial(UUID materialId) {
        this.materialId = materialId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMeasurementId() {
        return measurementId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getAreaM2() {
        return areaM2;
    }

    public BigDecimal getLarguraM() {
        return larguraM;
    }

    public BigDecimal getComprimentoM() {
        return comprimentoM;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getFatorPerdaAplicado() {
        return fatorPerdaAplicado;
    }

    public BigDecimal getEspessuraM() {
        return espessuraM;
    }

    public String getObservacao() {
        return observacao;
    }
}
