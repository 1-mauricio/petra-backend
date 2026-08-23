package com.marmorarias.delivery.adapter.web;

import com.marmorarias.delivery.adapter.persistence.DeliveryEntity;
import com.marmorarias.delivery.application.DeliveryService;
import com.marmorarias.delivery.domain.DeliveryStatus;
import com.marmorarias.identity.adapter.security.CurrentTenant;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final CurrentTenant currentTenant;

    public DeliveryController(DeliveryService deliveryService, CurrentTenant currentTenant) {
        this.deliveryService = deliveryService;
        this.currentTenant = currentTenant;
    }

    public record ScheduleRequest(UUID orderId, Instant dataAgendada) {
    }

    public record UpdateStatusRequest(DeliveryStatus status) {
    }

    @GetMapping("/deliveries")
    public List<DeliveryEntity> listar(@RequestParam UUID orderId) {
        return deliveryService.listarPorPedido(currentTenant.get(), orderId);
    }

    @GetMapping("/entregas")
    public List<DeliveryEntity> listarEntregas() {
        return deliveryService.listarPorOrganizacao(currentTenant.get());
    }

    @PatchMapping("/entregas/{id}")
    @PreAuthorize("hasAnyRole('admin', 'producao')")
    public DeliveryEntity atualizarStatusEntrega(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        return deliveryService.atualizarStatus(currentTenant.get(), id, request.status());
    }

    @PostMapping("/deliveries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'producao')")
    public DeliveryEntity agendar(@RequestBody ScheduleRequest request) {
        return deliveryService.agendar(currentTenant.get(), request.orderId(), request.dataAgendada());
    }

    @PutMapping("/deliveries/{id}/status")
    @PreAuthorize("hasAnyRole('admin', 'producao')")
    public DeliveryEntity atualizarStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        return deliveryService.atualizarStatus(currentTenant.get(), id, request.status());
    }
}
