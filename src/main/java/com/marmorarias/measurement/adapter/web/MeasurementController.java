package com.marmorarias.measurement.adapter.web;

import com.marmorarias.identity.adapter.security.CurrentTenant;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.measurement.adapter.persistence.MeasurementEntity;
import com.marmorarias.measurement.adapter.persistence.MeasurementListItem;
import com.marmorarias.measurement.adapter.persistence.MeasurementPieceEntity;
import com.marmorarias.measurement.application.AprovarMedicaoRequest;
import com.marmorarias.measurement.application.MeasurementApprovalResult;
import com.marmorarias.measurement.application.MeasurementService;
import com.marmorarias.measurement.application.RegistrarMedicaoCampoRequest;
import com.marmorarias.measurement.application.RegistrarMedicaoCampoResponse;
import com.marmorarias.measurement.application.RegistrarMedicaoRequest;
import com.marmorarias.orders.adapter.persistence.CustomerOrderEntity;
import com.marmorarias.orders.adapter.persistence.CustomerOrderRepository;
import com.marmorarias.orders.application.OrderService;
import com.marmorarias.quoting.application.CriarOrcamentoRequest;
import com.marmorarias.quoting.application.PecaRequest;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeasurementController {

    private final MeasurementService measurementService;
    private final OrderService orderService;
    private final CustomerOrderRepository customerOrderRepository;
    private final CurrentTenant currentTenant;

    public MeasurementController(MeasurementService measurementService, OrderService orderService,
                                  CustomerOrderRepository customerOrderRepository, CurrentTenant currentTenant) {
        this.measurementService = measurementService;
        this.orderService = orderService;
        this.customerOrderRepository = customerOrderRepository;
        this.currentTenant = currentTenant;
    }

    @GetMapping("/medicoes")
    public List<MeasurementListItem> listar() {
        return measurementService.listar(currentTenant.get());
    }

    @PostMapping("/measurements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'producao')")
    public MeasurementEntity registrar(@RequestBody RegistrarMedicaoRequest request) {
        TenantContext tenant = currentTenant.get();
        return measurementService.registrarMedicao(tenant, request.orderId(), tenant.userId(), request.pecas());
    }

    /** Endpoint que o medicao-pwa chama ao sincronizar (ver medicao-pwa/sync.js). */
    @PostMapping("/pedidos/{pedidoId}/medicoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'producao')")
    public RegistrarMedicaoCampoResponse registrarCampo(@PathVariable UUID pedidoId,
                                                         @RequestBody RegistrarMedicaoCampoRequest request) {
        TenantContext tenant = currentTenant.get();
        MeasurementEntity measurement = measurementService.registrarMedicaoCampo(tenant, pedidoId, tenant.userId(),
                request.pecas());
        return new RegistrarMedicaoCampoResponse(measurement.getId());
    }

    /**
     * Se a divergência exceder a tolerância, empurra o pedido para REVISAO_ORCAMENTO aqui — no
     * controller, não dentro do MeasurementService, para não criar uma dependência circular entre
     * os contextos measurement e orders (orders já depende de measurement para o guard de PRODUCAO).
     */
    @PostMapping("/measurements/{id}/aprovar")
    @PreAuthorize("hasAnyRole('admin', 'producao')")
    public MeasurementApprovalResult aprovar(@PathVariable UUID id, @RequestBody AprovarMedicaoRequest request) {
        TenantContext tenant = currentTenant.get();
        MeasurementApprovalResult resultado = measurementService.aprovarMedicao(tenant, id, request);
        if (resultado.exigeRevisaoOrcamento()) {
            UUID orderId = resultado.measurement().getOrderId();
            CustomerOrderEntity order = customerOrderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado: " + orderId));
            CriarOrcamentoRequest novoOrcamento = paraOrcamentoRevisado(order.getCustomerId(), resultado.pecas(), request);
            orderService.forcarRevisaoOrcamento(tenant, orderId, novoOrcamento,
                    "Divergência medição x orçamento acima da tolerância");
        }
        return resultado;
    }

    private CriarOrcamentoRequest paraOrcamentoRevisado(UUID customerId, List<MeasurementPieceEntity> pecasMedidas,
                                                          AprovarMedicaoRequest request) {
        List<PecaRequest> pecas = pecasMedidas.stream()
                .map(p -> new PecaRequest(p.getMaterialId(), p.getLarguraM(),
                        p.getComprimentoM().multiply(java.math.BigDecimal.valueOf(p.getQuantidade())), List.of(), List.of()))
                .toList();
        return new CriarOrcamentoRequest(customerId, null, pecas, request.fatorPerdaPorMaterial(),
                request.fatorPerdaDefault(), request.margem(), request.desconto(), request.maoDeObraCatalogItemId(),
                request.maoDeObraHoras());
    }
}
