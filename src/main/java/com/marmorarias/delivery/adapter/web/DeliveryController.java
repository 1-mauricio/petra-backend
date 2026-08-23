package com.marmorarias.delivery.adapter.web;

import com.marmorarias.delivery.adapter.persistence.DeliveryEntity;
import com.marmorarias.delivery.application.DeliveryService;
import com.marmorarias.delivery.domain.DeliveryStatus;
import com.marmorarias.identity.adapter.security.CurrentTenant;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final CurrentTenant currentTenant;

    public DeliveryController(DeliveryService deliveryService, CurrentTenant currentTenant) {
        this.deliveryService = deliveryService;
        this.currentTenant = currentTenant;
    }

    private static final Set<String> TIPOS_ACEITOS = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");
    private static final long TAMANHO_MAXIMO_BYTES = 10 * 1024 * 1024;

    public record ScheduleRequest(UUID orderId, Instant dataAgendada) {
    }

    public record UpdateStatusRequest(DeliveryStatus status) {
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

    @PostMapping("/entregas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'producao')")
    public DeliveryEntity agendar(@RequestBody ScheduleRequest request) {
        return deliveryService.agendar(currentTenant.get(), request.orderId(), request.dataAgendada());
    }

    @PostMapping("/entregas/{id}/comprovante")
    @PreAuthorize("hasAnyRole('admin', 'producao')")
    public DeliveryEntity enviarComprovante(@PathVariable UUID id, @RequestParam("comprovante") MultipartFile arquivo)
            throws IOException {
        if (arquivo.isEmpty()) {
            throw new IllegalArgumentException("Comprovante vazio");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("Comprovante maior que 10MB");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_ACEITOS.contains(contentType)) {
            throw new IllegalArgumentException("Tipo de arquivo não suportado: " + contentType);
        }
        String extensao = contentType.equals("application/pdf") ? ".pdf" : "." + contentType.substring(contentType.indexOf('/') + 1);
        return deliveryService.registrarComprovante(currentTenant.get(), id, arquivo.getBytes(), contentType, extensao);
    }
}
