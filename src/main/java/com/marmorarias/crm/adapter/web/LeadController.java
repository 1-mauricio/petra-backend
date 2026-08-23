package com.marmorarias.crm.adapter.web;

import com.marmorarias.crm.adapter.persistence.LeadEntity;
import com.marmorarias.crm.adapter.persistence.LeadListItem;
import com.marmorarias.crm.application.CrmService;
import com.marmorarias.crm.domain.LeadStatus;
import com.marmorarias.identity.adapter.security.CurrentTenant;
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
public class LeadController {

    private final CrmService crmService;
    private final CurrentTenant currentTenant;

    public LeadController(CrmService crmService, CurrentTenant currentTenant) {
        this.crmService = crmService;
        this.currentTenant = currentTenant;
    }

    public record CreateLeadRequest(UUID customerId, String origem) {
    }

    public record MotivoRequest(String motivo) {
    }

    public record UpdateStatusRequest(LeadStatus status, String motivoPerda) {
    }

    @GetMapping("/leads")
    public List<LeadListItem> listar() {
        return crmService.listarLeads(currentTenant.get());
    }

    @PatchMapping("/leads/{id}")
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public LeadEntity mover(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        return crmService.moverStatus(currentTenant.get(), id, request.status(), request.motivoPerda());
    }

    @PostMapping("/leads")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public LeadEntity criar(@RequestBody CreateLeadRequest request) {
        return crmService.criarLead(currentTenant.get(), request.customerId(), request.origem());
    }

    @PostMapping("/leads/{id}/ganho")
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public LeadEntity marcarGanho(@PathVariable UUID id) {
        return crmService.marcarLeadGanho(currentTenant.get(), id);
    }

    @PostMapping("/leads/{id}/perdido")
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public LeadEntity marcarPerdido(@PathVariable UUID id, @RequestBody MotivoRequest request) {
        return crmService.marcarLeadPerdido(currentTenant.get(), id, request.motivo());
    }
}
