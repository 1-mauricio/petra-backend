package com.marmorarias.delivery.application;

import com.marmorarias.channels.StoragePort;
import com.marmorarias.delivery.adapter.persistence.DeliveryEntity;
import com.marmorarias.delivery.adapter.persistence.DeliveryRepository;
import com.marmorarias.delivery.domain.DeliveryStatus;
import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.production.adapter.persistence.ProductionTaskEntity;
import com.marmorarias.production.adapter.persistence.ProductionTaskRepository;
import com.marmorarias.production.domain.ProductionTaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private final RlsContext rlsContext;
    private final DeliveryRepository deliveryRepository;
    private final StoragePort storagePort;
    private final ProductionTaskRepository productionTaskRepository;

    public DeliveryService(RlsContext rlsContext, DeliveryRepository deliveryRepository, StoragePort storagePort,
                            ProductionTaskRepository productionTaskRepository) {
        this.rlsContext = rlsContext;
        this.deliveryRepository = deliveryRepository;
        this.storagePort = storagePort;
        this.productionTaskRepository = productionTaskRepository;
    }

    /** Guard: só agenda entrega com produção "pronta" — ao menos uma tarefa e todas CONCLUIDA. */
    @Transactional
    public DeliveryEntity agendar(TenantContext tenant, UUID orderId, Instant dataAgendada) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        List<ProductionTaskEntity> tarefas = productionTaskRepository.findByOrderId(orderId);
        boolean pronta = !tarefas.isEmpty()
                && tarefas.stream().allMatch(t -> t.getStatus() == ProductionTaskStatus.CONCLUIDA);
        if (!pronta) {
            throw new IllegalStateException("Produção ainda não concluída para este pedido");
        }
        return deliveryRepository.save(new DeliveryEntity(tenant.organizationId(), orderId, dataAgendada));
    }

    @Transactional(readOnly = true)
    public List<DeliveryEntity> listarPorPedido(TenantContext tenant, UUID orderId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return deliveryRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<DeliveryEntity> listarPorOrganizacao(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return deliveryRepository.findByOrganizationId(tenant.organizationId());
    }

    @Transactional
    public DeliveryEntity atualizarStatus(TenantContext tenant, UUID deliveryId, DeliveryStatus status) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        DeliveryEntity delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Entrega não encontrada: " + deliveryId));
        if (status == DeliveryStatus.ENTREGUE) {
            delivery.marcarEntregue();
        } else {
            delivery.atualizarStatus(status);
        }
        return delivery;
    }

    @Transactional
    public DeliveryEntity registrarComprovante(TenantContext tenant, UUID deliveryId, byte[] conteudo,
                                                String contentType, String extensao) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        DeliveryEntity delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Entrega não encontrada: " + deliveryId));
        String path = tenant.organizationId() + "/" + deliveryId + "-" + Instant.now().toEpochMilli() + extensao;
        String url = storagePort.upload(path, conteudo, contentType);
        delivery.registrarComprovante(url);
        return delivery;
    }
}
