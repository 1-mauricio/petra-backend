package com.marmorarias.platformadmin.adapter.web;

import com.marmorarias.identity.adapter.security.PlatformAdminContext;
import com.marmorarias.platformadmin.adapter.persistence.OrganizationEntity;
import com.marmorarias.platformadmin.application.AdminOrgService;
import com.marmorarias.platformadmin.application.AdminOrgService.OrgSummary;
import com.marmorarias.platformbilling.domain.OrgBillingStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/organizacoes")
@PreAuthorize("hasRole('platform_admin')")
public class AdminOrgController {

    private final AdminOrgService adminOrgService;
    private final PlatformAdminContext platformAdminContext;

    public AdminOrgController(AdminOrgService adminOrgService, PlatformAdminContext platformAdminContext) {
        this.adminOrgService = adminOrgService;
        this.platformAdminContext = platformAdminContext;
    }

    public record CriarOrganizacaoRequest(String nome, String cnpj) {
    }

    public record AtualizarOrganizacaoRequest(String nome, String cnpj, OrgBillingStatus billingStatus) {
    }

    @GetMapping
    public List<OrgSummary> listar() {
        return adminOrgService.listar();
    }

    @GetMapping("/{orgId}")
    public OrgSummary buscar(@PathVariable UUID orgId) {
        return adminOrgService.buscar(orgId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationEntity criar(@RequestBody CriarOrganizacaoRequest request) {
        return adminOrgService.criar(platformAdminContext.userId(), request.nome(), request.cnpj());
    }

    @PatchMapping("/{orgId}")
    public OrganizationEntity atualizar(@PathVariable UUID orgId, @RequestBody AtualizarOrganizacaoRequest request) {
        return adminOrgService.atualizar(platformAdminContext.userId(), orgId, request.nome(), request.cnpj(),
                request.billingStatus());
    }

    @DeleteMapping("/{orgId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID orgId) {
        adminOrgService.excluir(platformAdminContext.userId(), orgId);
    }
}
