package com.marmorarias.quoting.adapter.web;

import com.marmorarias.identity.adapter.security.CurrentTenant;
import com.marmorarias.quoting.adapter.persistence.CatalogItemEntity;
import com.marmorarias.quoting.adapter.persistence.MaterialEntity;
import com.marmorarias.quoting.adapter.persistence.QuoteDetail;
import com.marmorarias.quoting.adapter.persistence.QuoteLineItemEntity;
import com.marmorarias.quoting.adapter.persistence.QuoteSummary;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionEntity;
import com.marmorarias.quoting.adapter.persistence.UnidadeMedida;
import com.marmorarias.quoting.application.CriarOrcamentoRequest;
import com.marmorarias.quoting.application.QuoteService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QuoteController {

    private final QuoteService quoteService;
    private final CurrentTenant currentTenant;

    public QuoteController(QuoteService quoteService, CurrentTenant currentTenant) {
        this.quoteService = quoteService;
        this.currentTenant = currentTenant;
    }

    public record NovaVersaoRequest(UUID baseadaEm) {
    }

    public record AdicionarItemRequest(UUID materialId, UUID catalogItemId, String descricao, BigDecimal quantidade,
                                        UnidadeMedida unidade) {
    }

    @GetMapping("/orcamentos")
    public List<QuoteSummary> listar() {
        return quoteService.listar(currentTenant.get());
    }

    @GetMapping("/clientes/{customerId}/orcamentos")
    public List<QuoteSummary> listarPorCliente(@PathVariable UUID customerId) {
        return quoteService.listarPorCliente(currentTenant.get(), customerId);
    }

    @GetMapping("/orcamentos/{quoteId}")
    public QuoteDetail buscarDetalhe(@PathVariable UUID quoteId) {
        return quoteService.buscarDetalhe(currentTenant.get(), quoteId);
    }

    @GetMapping("/materiais")
    public List<MaterialEntity> listarMateriais() {
        return quoteService.listarMateriais(currentTenant.get());
    }

    @GetMapping("/catalogo")
    public List<CatalogItemEntity> listarCatalogo() {
        return quoteService.listarCatalogo(currentTenant.get());
    }

    @PostMapping("/orcamentos/{quoteId}/versoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public QuoteVersionEntity novaVersao(@PathVariable UUID quoteId, @RequestBody NovaVersaoRequest request) {
        return quoteService.novaVersaoBaseadaEm(currentTenant.get(), quoteId, request.baseadaEm());
    }

    @PostMapping("/orcamentos/{quoteId}/versoes/{versionId}/enviar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public void enviarVersao(@PathVariable UUID quoteId, @PathVariable UUID versionId) {
        quoteService.enviar(currentTenant.get(), versionId);
    }

    @PostMapping("/orcamentos/{quoteId}/versoes/{versionId}/aprovar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public void aprovarVersao(@PathVariable UUID quoteId, @PathVariable UUID versionId) {
        quoteService.aprovar(currentTenant.get(), versionId);
    }

    @PostMapping("/orcamentos/{quoteId}/versoes/{versionId}/rejeitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public void rejeitarVersao(@PathVariable UUID quoteId, @PathVariable UUID versionId) {
        quoteService.rejeitar(currentTenant.get(), versionId);
    }

    @PostMapping("/orcamentos/{quoteId}/versoes/{versionId}/itens")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public QuoteLineItemEntity adicionarItem(@PathVariable UUID quoteId, @PathVariable UUID versionId,
                                              @RequestBody AdicionarItemRequest request) {
        return quoteService.adicionarItem(currentTenant.get(), versionId, request.materialId(),
                request.catalogItemId(), request.descricao(), request.quantidade(), request.unidade());
    }

    @DeleteMapping("/orcamentos/{quoteId}/versoes/{versionId}/itens/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public void removerItem(@PathVariable UUID quoteId, @PathVariable UUID versionId, @PathVariable UUID itemId) {
        quoteService.removerItem(currentTenant.get(), versionId, itemId);
    }

    @PostMapping("/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public QuoteVersionEntity criar(@RequestBody CriarOrcamentoRequest request) {
        return quoteService.criarOrcamento(currentTenant.get(), request);
    }

    @PutMapping("/quotes/{quoteId}")
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public QuoteVersionEntity revisar(@PathVariable UUID quoteId, @RequestBody CriarOrcamentoRequest request) {
        return quoteService.revisar(currentTenant.get(), quoteId, request);
    }

    @GetMapping("/quote-versions/{id}")
    public QuoteVersionEntity buscar(@PathVariable UUID id) {
        return quoteService.buscarVersao(currentTenant.get(), id);
    }

    @PostMapping("/quote-versions/{id}/enviar")
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public void enviar(@PathVariable UUID id) {
        quoteService.enviar(currentTenant.get(), id);
    }

    @PostMapping("/quote-versions/{id}/aprovar")
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public void aprovar(@PathVariable UUID id) {
        quoteService.aprovar(currentTenant.get(), id);
    }
}
