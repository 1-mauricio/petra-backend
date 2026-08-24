package com.marmorarias.identity.adapter.web;

import com.marmorarias.identity.adapter.persistence.OrgSettingsEntity;
import com.marmorarias.identity.adapter.security.CurrentTenant;
import com.marmorarias.identity.application.OrgSettingsService;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/org-settings")
public class OrgSettingsController {

    private final OrgSettingsService orgSettingsService;
    private final CurrentTenant currentTenant;

    public OrgSettingsController(OrgSettingsService orgSettingsService, CurrentTenant currentTenant) {
        this.orgSettingsService = orgSettingsService;
        this.currentTenant = currentTenant;
    }

    public record AtualizarOrgSettingsRequest(BigDecimal toleranciaPerc, BigDecimal toleranciaAbs,
                                               BigDecimal descontoLimitePerc, Map<String, BigDecimal> fatorPerda) {
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'comercial', 'producao')")
    public OrgSettingsEntity buscar() {
        return orgSettingsService.buscar(currentTenant.get());
    }

    @PatchMapping
    @PreAuthorize("hasRole('admin')")
    public OrgSettingsEntity atualizar(@RequestBody AtualizarOrgSettingsRequest request) {
        return orgSettingsService.atualizar(currentTenant.get(), request.toleranciaPerc(), request.toleranciaAbs(),
                request.descontoLimitePerc(), request.fatorPerda());
    }
}
