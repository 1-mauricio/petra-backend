package com.marmorarias.delivery.application;

import com.marmorarias.delivery.adapter.persistence.DeliveryEntity;
import com.marmorarias.delivery.adapter.persistence.DeliveryRepository;
import com.marmorarias.delivery.domain.DeliveryStatus;
import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.identity.domain.TenantContext;
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

    public DeliveryService(RlsContext rlsContext, DeliveryRepository deliveryRepository) {
        this.rlsContext = rlsContext;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public DeliveryEntity agendar(TenantContext tenant, UUID orderId, Instant dataAgendada) {
        rlsContext.setCurrentOrg(tenant.organizationId());
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
}
