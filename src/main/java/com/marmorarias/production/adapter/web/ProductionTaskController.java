package com.marmorarias.production.adapter.web;

import com.marmorarias.identity.adapter.security.CurrentTenant;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.production.adapter.persistence.ProductionTaskEntity;
import com.marmorarias.production.application.ProductionService;
import com.marmorarias.production.domain.ProductionTaskStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProductionTaskController {

    private final ProductionService productionService;
    private final CurrentTenant currentTenant;

    public ProductionTaskController(ProductionService productionService, CurrentTenant currentTenant) {
        this.productionService = productionService;
        this.currentTenant = currentTenant;
    }

    public record CreateTaskRequest(UUID orderId, String descricao, UUID responsavel) {
    }

    public record UpdateStatusRequest(ProductionTaskStatus status) {
    }

    @GetMapping("/producao/tarefas")
    public List<ProductionTaskEntity> listarTarefas() {
        return productionService.listarPorOrganizacao(currentTenant.get());
    }

    @PatchMapping("/producao/tarefas/{id}")
    @PreAuthorize("hasAnyRole('admin', 'producao')")
    public ProductionTaskEntity atualizarStatusTarefa(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        return productionService.atualizarStatus(currentTenant.get(), id, request.status());
    }

    @PostMapping("/producao/tarefas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'producao')")
    public ProductionTaskEntity criar(@RequestBody CreateTaskRequest request) {
        TenantContext tenant = currentTenant.get();
        return productionService.criar(tenant, request.orderId(), request.descricao(), request.responsavel());
    }
}
