package com.marmorarias.quoting.application;

import com.marmorarias.crm.adapter.persistence.CustomerEntity;
import com.marmorarias.crm.adapter.persistence.CustomerRepository;
import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.quoting.adapter.persistence.CatalogItemEntity;
import com.marmorarias.quoting.adapter.persistence.CatalogItemRepository;
import com.marmorarias.quoting.adapter.persistence.CatalogItemTipo;
import com.marmorarias.quoting.adapter.persistence.MaterialEntity;
import com.marmorarias.quoting.adapter.persistence.MaterialRepository;
import com.marmorarias.quoting.adapter.persistence.QuoteDetail;
import com.marmorarias.quoting.adapter.persistence.QuoteEntity;
import com.marmorarias.quoting.adapter.persistence.QuoteLineItemEntity;
import com.marmorarias.quoting.adapter.persistence.QuoteLineItemRepository;
import com.marmorarias.quoting.adapter.persistence.QuoteRepository;
import com.marmorarias.quoting.adapter.persistence.QuoteSummary;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionDetail;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionEntity;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionRepository;
import com.marmorarias.quoting.adapter.persistence.UnidadeMedida;
import com.marmorarias.quoting.domain.OrcamentoCalculator;
import com.marmorarias.quoting.domain.OrcamentoCalculator.Acabamento;
import com.marmorarias.quoting.domain.OrcamentoCalculator.CatalogItemSnapshot;
import com.marmorarias.quoting.domain.OrcamentoCalculator.CatalogoSnapshot;
import com.marmorarias.quoting.domain.OrcamentoCalculator.MaterialSnapshot;
import com.marmorarias.quoting.domain.OrcamentoCalculator.OrcamentoParams;
import com.marmorarias.quoting.domain.OrcamentoCalculator.OrcamentoResultado;
import com.marmorarias.quoting.domain.OrcamentoCalculator.Peca;
import com.marmorarias.quoting.domain.OrcamentoCalculator.Recorte;
import com.marmorarias.quoting.domain.QuoteVersionStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra criação e versionamento de orçamentos usando o motor de cálculo puro
 * (OrcamentoCalculator). Editar RASCUNHO é edição simples; editar versão já ENVIADO/APROVADO
 * cria v+1 com line items re-snapshotados, deixando a anterior read-only (reforçado também por
 * trigger no banco).
 */
@Service
public class QuoteService {

    private final RlsContext rlsContext;
    private final QuoteRepository quoteRepository;
    private final QuoteVersionRepository quoteVersionRepository;
    private final QuoteLineItemRepository quoteLineItemRepository;
    private final MaterialRepository materialRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final CustomerRepository customerRepository;

    public QuoteService(RlsContext rlsContext, QuoteRepository quoteRepository,
                         QuoteVersionRepository quoteVersionRepository, QuoteLineItemRepository quoteLineItemRepository,
                         MaterialRepository materialRepository, CatalogItemRepository catalogItemRepository,
                         CustomerRepository customerRepository) {
        this.rlsContext = rlsContext;
        this.quoteRepository = quoteRepository;
        this.quoteVersionRepository = quoteVersionRepository;
        this.quoteLineItemRepository = quoteLineItemRepository;
        this.materialRepository = materialRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<QuoteSummary> listar(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return resumir(quoteRepository.findByOrganizationIdOrderByCreatedAtDesc(tenant.organizationId()));
    }

    @Transactional(readOnly = true)
    public List<QuoteSummary> listarPorCliente(TenantContext tenant, UUID customerId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return resumir(quoteRepository.findByCustomerIdOrderByCreatedAtDesc(customerId));
    }

    private List<QuoteSummary> resumir(List<QuoteEntity> quotes) {
        List<QuoteSummary> resumo = new ArrayList<>();
        for (QuoteEntity quote : quotes) {
            QuoteVersionEntity ultima = quoteVersionRepository
                    .findTopByQuoteIdOrderByVersionNumberDesc(quote.getId())
                    .orElse(null);
            if (ultima == null) {
                continue;
            }
            String customerNome = customerRepository.findById(quote.getCustomerId())
                    .map(CustomerEntity::getNome)
                    .orElse(null);
            resumo.add(new QuoteSummary(quote.getId(), customerNome, ultima.getVersionNumber(), ultima.getStatus(),
                    ultima.getValorTotal(), quote.getCreatedAt()));
        }
        return resumo;
    }

    @Transactional(readOnly = true)
    public QuoteDetail buscarDetalhe(TenantContext tenant, UUID quoteId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        QuoteEntity quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new NoSuchElementException("Orçamento não encontrado: " + quoteId));
        String customerNome = customerRepository.findById(quote.getCustomerId())
                .map(CustomerEntity::getNome)
                .orElse(null);
        List<QuoteVersionDetail> versions = new ArrayList<>();
        for (QuoteVersionEntity v : quoteVersionRepository.findByQuoteIdOrderByVersionNumberDesc(quoteId)) {
            versions.add(new QuoteVersionDetail(v.getId(), v.getQuoteId(), v.getVersionNumber(), v.getStatus(),
                    v.getValorTotal(), v.getCreatedAt(), v.getApprovedAt(),
                    quoteLineItemRepository.findByQuoteVersionId(v.getId())));
        }
        return new QuoteDetail(quote.getId(), quote.getCustomerId(), customerNome, quote.getLeadId(),
                quote.getCreatedAt(), versions);
    }

    @Transactional(readOnly = true)
    public List<MaterialEntity> listarMateriais(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return materialRepository.findByOrganizationId(tenant.organizationId());
    }

    @Transactional(readOnly = true)
    public List<CatalogItemEntity> listarCatalogo(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return catalogItemRepository.findByOrganizationId(tenant.organizationId());
    }

    @Transactional
    public MaterialEntity criarMaterial(TenantContext tenant, String tipo, String cor, BigDecimal precoM2,
                                         BigDecimal larguraChapa, BigDecimal comprimentoChapa) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return materialRepository.save(new MaterialEntity(tenant.organizationId(), tipo, cor, precoM2, larguraChapa,
                comprimentoChapa));
    }

    /** Reajuste aqui não altera orçamentos existentes: QuoteLineItemEntity guarda o preço snapshotado na criação. */
    @Transactional
    public MaterialEntity atualizarMaterial(TenantContext tenant, UUID id, BigDecimal precoM2, Boolean ativo) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        MaterialEntity material = materialRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Material não encontrado: " + id));
        if (precoM2 != null) {
            material.atualizarPreco(precoM2);
        }
        if (ativo != null) {
            material.definirAtivo(ativo);
        }
        return material;
    }

    @Transactional
    public CatalogItemEntity criarItemCatalogo(TenantContext tenant, CatalogItemTipo tipo, String descricao,
                                                UnidadeMedida unidade, BigDecimal preco) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return catalogItemRepository.save(new CatalogItemEntity(tenant.organizationId(), tipo, descricao, unidade, preco));
    }

    @Transactional
    public CatalogItemEntity atualizarItemCatalogo(TenantContext tenant, UUID id, BigDecimal preco, Boolean ativo) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        CatalogItemEntity item = catalogItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item de catálogo não encontrado: " + id));
        if (preco != null) {
            item.atualizarPreco(preco);
        }
        if (ativo != null) {
            item.definirAtivo(ativo);
        }
        return item;
    }

    @Transactional
    public void rejeitar(TenantContext tenant, UUID quoteVersionId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        buscarVersao(quoteVersionId).rejeitar();
    }

    /** Nova versão RASCUNHO copiando as linhas (e o total) da versão informada — edição livre a partir daí. */
    @Transactional
    public QuoteVersionEntity novaVersaoBaseadaEm(TenantContext tenant, UUID quoteId, UUID baseadaEmVersionId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        QuoteVersionEntity base = buscarVersao(baseadaEmVersionId);
        QuoteVersionEntity ultima = quoteVersionRepository.findTopByQuoteIdOrderByVersionNumberDesc(quoteId)
                .orElseThrow(() -> new NoSuchElementException("Orçamento sem versão: " + quoteId));
        QuoteVersionEntity nova = quoteVersionRepository.save(
                new QuoteVersionEntity(tenant.organizationId(), quoteId, ultima.getVersionNumber() + 1, base.getValorTotal()));
        List<QuoteLineItemEntity> copiadas = quoteLineItemRepository.findByQuoteVersionId(base.getId()).stream()
                .map(i -> new QuoteLineItemEntity(tenant.organizationId(), nova.getId(), i.getMaterialId(),
                        i.getCatalogItemId(), i.getDescricao(), i.getQuantidade(), i.getUnidade(),
                        i.getPrecoUnitarioSnapshot(), i.getSubtotal()))
                .toList();
        quoteLineItemRepository.saveAll(copiadas);
        return nova;
    }

    /** Preço vem do snapshot atual do material/catalog_item (invariante 4) — só é lido, nunca recalculado depois. */
    @Transactional
    public QuoteLineItemEntity adicionarItem(TenantContext tenant, UUID quoteVersionId, UUID materialId,
                                              UUID catalogItemId, String descricao, BigDecimal quantidade,
                                              UnidadeMedida unidade) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        QuoteVersionEntity versao = buscarVersao(quoteVersionId);

        BigDecimal precoUnitario;
        if (materialId != null) {
            precoUnitario = materialRepository.findById(materialId)
                    .orElseThrow(() -> new NoSuchElementException("Material não encontrado: " + materialId))
                    .getPrecoM2();
        } else {
            precoUnitario = buscarCatalogItem(catalogItemId).getPreco();
        }
        BigDecimal subtotal = precoUnitario.multiply(quantidade);

        QuoteLineItemEntity item = quoteLineItemRepository.save(new QuoteLineItemEntity(tenant.organizationId(),
                quoteVersionId, materialId, catalogItemId, descricao, quantidade, unidade, precoUnitario, subtotal));
        versao.atualizarValorTotal(versao.getValorTotal().add(subtotal));
        return item;
    }

    @Transactional
    public void removerItem(TenantContext tenant, UUID quoteVersionId, UUID itemId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        QuoteVersionEntity versao = buscarVersao(quoteVersionId);
        QuoteLineItemEntity item = quoteLineItemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item não encontrado: " + itemId));
        quoteLineItemRepository.delete(item);
        versao.atualizarValorTotal(versao.getValorTotal().subtract(item.getSubtotal()));
    }

    @Transactional
    public QuoteVersionEntity criarOrcamento(TenantContext tenant, CriarOrcamentoRequest request) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        QuoteEntity quote = quoteRepository.save(
                new QuoteEntity(tenant.organizationId(), request.customerId(), request.leadId()));
        return criarVersao(tenant, quote.getId(), 1, request);
    }

    /** RASCUNHO recebe edição simples (linhas substituídas, mesma versão); qualquer outro status cria v+1. */
    @Transactional
    public QuoteVersionEntity revisar(TenantContext tenant, UUID quoteId, CriarOrcamentoRequest request) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        QuoteVersionEntity ultima = quoteVersionRepository.findTopByQuoteIdOrderByVersionNumberDesc(quoteId)
                .orElseThrow(() -> new NoSuchElementException("Orçamento sem versão: " + quoteId));

        if (ultima.getStatus() == QuoteVersionStatus.RASCUNHO) {
            quoteLineItemRepository.deleteAll(quoteLineItemRepository.findByQuoteVersionId(ultima.getId()));
            OrcamentoResultado resultado = calcular(tenant, request);
            gravarLinhas(tenant, ultima.getId(), request, resultado);
            ultima.atualizarValorTotal(resultado.precoCliente());
            return ultima;
        }

        return criarVersao(tenant, quoteId, ultima.getVersionNumber() + 1, request);
    }

    @Transactional
    public void enviar(TenantContext tenant, UUID quoteVersionId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        buscarVersao(quoteVersionId).enviar();
    }

    @Transactional
    public void aprovar(TenantContext tenant, UUID quoteVersionId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        buscarVersao(quoteVersionId).aprovar();
    }

    @Transactional(readOnly = true)
    public QuoteVersionEntity buscarVersao(TenantContext tenant, UUID quoteVersionId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return buscarVersao(quoteVersionId);
    }

    @Transactional(readOnly = true)
    public List<QuoteLineItemEntity> buscarLinhas(TenantContext tenant, UUID quoteVersionId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return quoteLineItemRepository.findByQuoteVersionId(quoteVersionId);
    }

    private QuoteVersionEntity buscarVersao(UUID quoteVersionId) {
        return quoteVersionRepository.findById(quoteVersionId)
                .orElseThrow(() -> new NoSuchElementException("quote_version não encontrada: " + quoteVersionId));
    }

    private QuoteVersionEntity criarVersao(TenantContext tenant, UUID quoteId, int versionNumber,
                                            CriarOrcamentoRequest request) {
        OrcamentoResultado resultado = calcular(tenant, request);
        QuoteVersionEntity versao = quoteVersionRepository.save(
                new QuoteVersionEntity(tenant.organizationId(), quoteId, versionNumber, resultado.precoCliente()));
        gravarLinhas(tenant, versao.getId(), request, resultado);
        return versao;
    }

    private OrcamentoResultado calcular(TenantContext tenant, CriarOrcamentoRequest request) {
        CatalogoSnapshot catalogo = carregarCatalogo(request);
        List<Peca> pecas = request.pecas().stream().map(this::paraPeca).toList();

        BigDecimal maoDeObra = BigDecimal.ZERO;
        if (request.maoDeObraCatalogItemId() != null) {
            BigDecimal preco = buscarCatalogItem(request.maoDeObraCatalogItemId()).getPreco();
            maoDeObra = preco.multiply(request.maoDeObraHoras());
        }

        OrcamentoParams params = new OrcamentoParams(chavesPorString(request.fatorPerdaPorMaterial()),
                request.fatorPerdaDefault(), request.margem(), request.desconto(), maoDeObra);
        return OrcamentoCalculator.calcularOrcamento(pecas, catalogo, params);
    }

    /**
     * O breakdown do calculador emprilha, na mesma ordem desta função, [peça, seus acabamentos,
     * seus recortes] por peça — por isso lemos com um cursor sequencial em vez de casar por índice
     * ou descrição (que podem colidir se dois itens repetirem o mesmo catalog_item_id).
     */
    private void gravarLinhas(TenantContext tenant, UUID quoteVersionId, CriarOrcamentoRequest request,
                               OrcamentoResultado resultado) {
        List<QuoteLineItemEntity> linhas = new ArrayList<>();
        int cursor = 0;
        for (int i = 0; i < request.pecas().size(); i++) {
            PecaRequest peca = request.pecas().get(i);
            MaterialEntity material = materialRepository.findById(peca.materialId())
                    .orElseThrow(() -> new NoSuchElementException("Material não encontrado: " + peca.materialId()));
            BigDecimal area = peca.largura().multiply(peca.comprimento());
            linhas.add(new QuoteLineItemEntity(tenant.organizationId(), quoteVersionId, material.getId(), null,
                    "peça " + i + " (" + material.getTipo() + " " + material.getCor() + ")", area,
                    UnidadeMedida.METRO_QUADRADO, material.getPrecoM2(), resultado.breakdown().get(cursor++).valor()));

            for (PecaRequest.AcabamentoRequest acabamento : peca.acabamentos()) {
                CatalogItemEntity item = buscarCatalogItem(acabamento.catalogItemId());
                linhas.add(new QuoteLineItemEntity(tenant.organizationId(), quoteVersionId, null, item.getId(),
                        "acabamento " + item.getDescricao(), acabamento.metrosLineares(), UnidadeMedida.METRO_LINEAR,
                        item.getPreco(), resultado.breakdown().get(cursor++).valor()));
            }
            for (PecaRequest.RecorteRequest recorte : peca.recortes()) {
                CatalogItemEntity item = buscarCatalogItem(recorte.catalogItemId());
                linhas.add(new QuoteLineItemEntity(tenant.organizationId(), quoteVersionId, null, item.getId(),
                        "recorte " + item.getDescricao(), recorte.quantidade(), UnidadeMedida.UNIDADE, item.getPreco(),
                        resultado.breakdown().get(cursor++).valor()));
            }
        }

        if (request.maoDeObraCatalogItemId() != null) {
            CatalogItemEntity item = buscarCatalogItem(request.maoDeObraCatalogItemId());
            linhas.add(new QuoteLineItemEntity(tenant.organizationId(), quoteVersionId, null, item.getId(),
                    "mão de obra: " + item.getDescricao(), request.maoDeObraHoras(), UnidadeMedida.HORA, item.getPreco(),
                    item.getPreco().multiply(request.maoDeObraHoras())));
        }

        quoteLineItemRepository.saveAll(linhas);
    }

    private CatalogItemEntity buscarCatalogItem(UUID id) {
        return catalogItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item de catálogo não encontrado: " + id));
    }

    private CatalogoSnapshot carregarCatalogo(CriarOrcamentoRequest request) {
        Set<UUID> materialIds = new LinkedHashSet<>();
        Set<UUID> catalogItemIds = new LinkedHashSet<>();
        for (PecaRequest peca : request.pecas()) {
            materialIds.add(peca.materialId());
            peca.acabamentos().forEach(a -> catalogItemIds.add(a.catalogItemId()));
            peca.recortes().forEach(r -> catalogItemIds.add(r.catalogItemId()));
        }
        if (request.maoDeObraCatalogItemId() != null) {
            catalogItemIds.add(request.maoDeObraCatalogItemId());
        }

        Map<String, MaterialSnapshot> materiais = new HashMap<>();
        for (MaterialEntity m : materialRepository.findByIdIn(List.copyOf(materialIds))) {
            materiais.put(m.getId().toString(),
                    new MaterialSnapshot(m.getPrecoM2(), m.getLarguraChapa(), m.getComprimentoChapa()));
        }
        Map<String, CatalogItemSnapshot> itens = new HashMap<>();
        for (CatalogItemEntity c : catalogItemRepository.findByIdIn(List.copyOf(catalogItemIds))) {
            itens.put(c.getId().toString(), new CatalogItemSnapshot(c.getPreco()));
        }
        return new CatalogoSnapshot(materiais, itens);
    }

    private Peca paraPeca(PecaRequest peca) {
        List<Acabamento> acabamentos = peca.acabamentos().stream()
                .map(a -> new Acabamento(a.catalogItemId().toString(), a.metrosLineares())).toList();
        List<Recorte> recortes = peca.recortes().stream()
                .map(r -> new Recorte(r.catalogItemId().toString(), r.quantidade())).toList();
        return new Peca(peca.materialId().toString(), peca.largura(), peca.comprimento(), acabamentos, recortes);
    }

    private Map<String, BigDecimal> chavesPorString(Map<UUID, BigDecimal> porUuid) {
        Map<String, BigDecimal> porString = new HashMap<>();
        if (porUuid != null) {
            porUuid.forEach((id, valor) -> porString.put(id.toString(), valor));
        }
        return porString;
    }
}
