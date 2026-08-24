package com.marmorarias.measurement.application;

import com.marmorarias.identity.adapter.persistence.OrgSettingsEntity;
import com.marmorarias.identity.adapter.persistence.OrgSettingsRepository;
import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.measurement.adapter.persistence.MeasurementEntity;
import com.marmorarias.measurement.adapter.persistence.MeasurementListItem;
import com.marmorarias.measurement.adapter.persistence.MeasurementPieceEntity;
import com.marmorarias.measurement.adapter.persistence.MeasurementPieceRepository;
import com.marmorarias.measurement.adapter.persistence.MeasurementRepository;
import com.marmorarias.measurement.domain.DivergenceChecker;
import com.marmorarias.orders.adapter.persistence.CustomerOrderEntity;
import com.marmorarias.orders.adapter.persistence.CustomerOrderRepository;
import com.marmorarias.quoting.adapter.persistence.CatalogItemEntity;
import com.marmorarias.quoting.adapter.persistence.CatalogItemRepository;
import com.marmorarias.quoting.adapter.persistence.MaterialEntity;
import com.marmorarias.quoting.adapter.persistence.MaterialRepository;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionEntity;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionRepository;
import com.marmorarias.quoting.domain.OrcamentoCalculator;
import com.marmorarias.quoting.domain.OrcamentoCalculator.CatalogItemSnapshot;
import com.marmorarias.quoting.domain.OrcamentoCalculator.CatalogoSnapshot;
import com.marmorarias.quoting.domain.OrcamentoCalculator.MaterialSnapshot;
import com.marmorarias.quoting.domain.OrcamentoCalculator.OrcamentoParams;
import com.marmorarias.quoting.domain.OrcamentoCalculator.Peca;
import com.marmorarias.quoting.domain.QuoteVersionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeasurementService {

    private final RlsContext rlsContext;
    private final MeasurementRepository measurementRepository;
    private final MeasurementPieceRepository measurementPieceRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final QuoteVersionRepository quoteVersionRepository;
    private final MaterialRepository materialRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final OrgSettingsRepository orgSettingsRepository;

    public MeasurementService(RlsContext rlsContext, MeasurementRepository measurementRepository,
                               MeasurementPieceRepository measurementPieceRepository,
                               CustomerOrderRepository customerOrderRepository,
                               QuoteVersionRepository quoteVersionRepository, MaterialRepository materialRepository,
                               CatalogItemRepository catalogItemRepository, OrgSettingsRepository orgSettingsRepository) {
        this.rlsContext = rlsContext;
        this.measurementRepository = measurementRepository;
        this.measurementPieceRepository = measurementPieceRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.quoteVersionRepository = quoteVersionRepository;
        this.materialRepository = materialRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.orgSettingsRepository = orgSettingsRepository;
    }

    @Transactional(readOnly = true)
    public List<MeasurementListItem> listar(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        List<MeasurementListItem> resultado = new java.util.ArrayList<>();
        for (MeasurementEntity m : measurementRepository.findByOrganizationIdOrderByDataMedicaoDesc(tenant.organizationId())) {
            List<MeasurementListItem.Peca> pecas = measurementPieceRepository.findByMeasurementId(m.getId()).stream()
                    .map(p -> new MeasurementListItem.Peca(p.getId(), p.getDescricao(), p.getLarguraM(),
                            p.getComprimentoM(), p.getQuantidade(), p.getAreaM2()))
                    .toList();
            resultado.add(new MeasurementListItem(m.getId(), m.getOrderId(), m.getStatus(), m.getDataAgendada(),
                    m.getDataMedicao(), m.getTecnicoResponsavel(), m.getApprovedAt(), pecas));
        }
        return resultado;
    }

    @Transactional
    public MeasurementEntity registrarMedicao(TenantContext tenant, UUID orderId, UUID tecnicoResponsavel,
                                               List<PecaMedidaRequest> pecas) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        MeasurementEntity measurement = measurementRepository.save(
                new MeasurementEntity(tenant.organizationId(), orderId, tecnicoResponsavel));
        for (PecaMedidaRequest peca : pecas) {
            measurementPieceRepository.save(new MeasurementPieceEntity(tenant.organizationId(), measurement.getId(),
                    peca.materialId(), peca.descricao(), peca.larguraM(), peca.comprimentoM(), peca.quantidade(),
                    peca.fatorPerdaAplicado()));
        }
        return measurement;
    }

    /**
     * Captura de campo (medicao-pwa): dimensões em cm, sem material — material é atribuído
     * depois, no escritório (ver V16). ponytail: sem lock por técnico — cada envio cria uma nova
     * medição PENDENTE mesmo que já exista outra para o mesmo pedido; upgrade quando o produto
     * precisar de exclusividade real por técnico.
     */
    @Transactional
    public MeasurementEntity registrarMedicaoCampo(TenantContext tenant, UUID orderId, UUID tecnicoResponsavel,
                                                     List<RegistrarMedicaoCampoRequest.PecaCampoRequest> pecas) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        MeasurementEntity measurement = measurementRepository.save(
                new MeasurementEntity(tenant.organizationId(), orderId, tecnicoResponsavel));
        for (RegistrarMedicaoCampoRequest.PecaCampoRequest peca : pecas) {
            measurementPieceRepository.save(new MeasurementPieceEntity(tenant.organizationId(), measurement.getId(),
                    peca.descricao(), cmParaM(peca.largura()), cmParaM(peca.altura()), cmParaM(peca.espessura()),
                    peca.observacao()));
        }
        return measurement;
    }

    private static BigDecimal cmParaM(BigDecimal cm) {
        return cm == null ? null : cm.divide(BigDecimal.valueOf(100));
    }

    /** Agenda a visita técnica (sem peças ainda) — primeiro estágio do fluxo de campo. */
    @Transactional
    public MeasurementEntity agendarMedicao(TenantContext tenant, UUID orderId, UUID tecnicoResponsavel,
                                             Instant dataAgendada) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return measurementRepository.save(
                new MeasurementEntity(tenant.organizationId(), orderId, tecnicoResponsavel, dataAgendada));
    }

    @Transactional
    public MeasurementEntity iniciarCampo(TenantContext tenant, UUID measurementId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        MeasurementEntity measurement = measurementRepository.findById(measurementId)
                .orElseThrow(() -> new NoSuchElementException("Medição não encontrada: " + measurementId));
        measurement.iniciarCampo();
        return measurement;
    }

    /** Anexa as peças medidas em campo e conclui a medição — libera para aprovação/rejeição. */
    @Transactional
    public MeasurementEntity concluirMedicao(TenantContext tenant, UUID measurementId, List<PecaMedidaRequest> pecas) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        MeasurementEntity measurement = measurementRepository.findById(measurementId)
                .orElseThrow(() -> new NoSuchElementException("Medição não encontrada: " + measurementId));
        for (PecaMedidaRequest peca : pecas) {
            measurementPieceRepository.save(new MeasurementPieceEntity(tenant.organizationId(), measurement.getId(),
                    peca.materialId(), peca.descricao(), peca.larguraM(), peca.comprimentoM(), peca.quantidade(),
                    peca.fatorPerdaAplicado()));
        }
        measurement.concluir();
        return measurement;
    }

    /**
     * Recalcula com as medidas reais via o mesmo motor de cálculo do orçamento (invariante 2) e
     * aprova a medição (imutável a partir daqui — invariante 5). Não decide sozinho a transição do
     * pedido: devolve se a divergência excede a tolerância para o chamador acionar REVISAO_ORCAMENTO.
     */
    @Transactional
    public MeasurementApprovalResult aprovarMedicao(TenantContext tenant, UUID measurementId,
                                                      AprovarMedicaoRequest request) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        MeasurementEntity measurement = measurementRepository.findById(measurementId)
                .orElseThrow(() -> new NoSuchElementException("Medição não encontrada: " + measurementId));
        List<MeasurementPieceEntity> pecas = measurementPieceRepository.findByMeasurementId(measurementId);

        CustomerOrderEntity order = customerOrderRepository.findById(measurement.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado: " + measurement.getOrderId()));
        QuoteVersionEntity quoteVersion = quoteVersionRepository.findById(order.getCurrentQuoteVersionId())
                .orElseThrow(() -> new NoSuchElementException("Orçamento não encontrado"));
        if (quoteVersion.getStatus() != QuoteVersionStatus.APROVADO) {
            throw new IllegalStateException("Orçamento ainda não aprovado — nada para comparar a divergência");
        }

        BigDecimal valorRecalculado = recalcular(pecas, request);

        OrgSettingsEntity settings = orgSettingsRepository.findById(tenant.organizationId())
                .orElseThrow(() -> new NoSuchElementException("org_settings não encontrado"));
        DivergenceChecker.Resultado divergencia = DivergenceChecker.avaliar(quoteVersion.getValorTotal(),
                valorRecalculado, settings.getToleranciaPerc(), settings.getToleranciaAbs());

        measurement.aprovar();
        return new MeasurementApprovalResult(measurement, pecas, valorRecalculado, divergencia);
    }

    @Transactional
    public MeasurementEntity rejeitarMedicao(TenantContext tenant, UUID measurementId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        MeasurementEntity measurement = measurementRepository.findById(measurementId)
                .orElseThrow(() -> new NoSuchElementException("Medição não encontrada: " + measurementId));
        measurement.rejeitar();
        return measurement;
    }

    private BigDecimal recalcular(List<MeasurementPieceEntity> pecasMedidas, AprovarMedicaoRequest request) {
        Map<String, MaterialSnapshot> materiais = new HashMap<>();
        List<UUID> materialIds = pecasMedidas.stream().map(MeasurementPieceEntity::getMaterialId).distinct().toList();
        for (MaterialEntity m : materialRepository.findByIdIn(materialIds)) {
            materiais.put(m.getId().toString(), new MaterialSnapshot(m.getPrecoM2(), m.getLarguraChapa(), m.getComprimentoChapa()));
        }

        Map<String, CatalogItemSnapshot> itens = new HashMap<>();
        BigDecimal maoDeObra = BigDecimal.ZERO;
        if (request.maoDeObraCatalogItemId() != null) {
            CatalogItemEntity item = catalogItemRepository.findById(request.maoDeObraCatalogItemId())
                    .orElseThrow(() -> new NoSuchElementException("Item de catálogo não encontrado"));
            itens.put(item.getId().toString(), new CatalogItemSnapshot(item.getPreco()));
            maoDeObra = item.getPreco().multiply(request.maoDeObraHoras());
        }

        List<Peca> pecas = pecasMedidas.stream()
                .map(p -> new Peca(p.getMaterialId().toString(), p.getLarguraM(),
                        p.getComprimentoM().multiply(BigDecimal.valueOf(p.getQuantidade())), List.of(), List.of()))
                .toList();

        Map<String, BigDecimal> fatorPerdaPorString = new HashMap<>();
        if (request.fatorPerdaPorMaterial() != null) {
            request.fatorPerdaPorMaterial().forEach((id, valor) -> fatorPerdaPorString.put(id.toString(), valor));
        }

        OrcamentoParams params = new OrcamentoParams(fatorPerdaPorString, request.fatorPerdaDefault(),
                request.margem(), request.desconto(), maoDeObra);
        return OrcamentoCalculator.calcularOrcamento(pecas, new CatalogoSnapshot(materiais, itens), params).precoCliente();
    }
}
