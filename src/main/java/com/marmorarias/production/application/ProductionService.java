package com.marmorarias.production.application;

import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.production.adapter.persistence.ProductionTaskEntity;
import com.marmorarias.production.adapter.persistence.ProductionTaskRepository;
import com.marmorarias.production.domain.ProductionTaskStatus;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionService {

    private final RlsContext rlsContext;
    private final ProductionTaskRepository productionTaskRepository;

    public ProductionService(RlsContext rlsContext, ProductionTaskRepository productionTaskRepository) {
        this.rlsContext = rlsContext;
        this.productionTaskRepository = productionTaskRepository;
    }

    @Transactional
    public ProductionTaskEntity criar(TenantContext tenant, UUID orderId, String descricao, UUID responsavel) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return productionTaskRepository.save(
                new ProductionTaskEntity(tenant.organizationId(), orderId, descricao, responsavel));
    }

    @Transactional(readOnly = true)
    public List<ProductionTaskEntity> listarPorPedido(TenantContext tenant, UUID orderId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return productionTaskRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<ProductionTaskEntity> listarPorOrganizacao(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return productionTaskRepository.findByOrganizationId(tenant.organizationId());
    }

    @Transactional
    public ProductionTaskEntity atualizarStatus(TenantContext tenant, UUID taskId, ProductionTaskStatus status) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        ProductionTaskEntity task = productionTaskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Tarefa de produção não encontrada: " + taskId));
        task.atualizarStatus(status);
        return task;
    }
}
