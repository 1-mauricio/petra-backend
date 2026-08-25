package com.marmorarias.platformadmin.adapter.web;

import com.marmorarias.identity.adapter.persistence.UserProfileEntity;
import com.marmorarias.identity.adapter.security.PlatformAdminContext;
import com.marmorarias.identity.domain.Role;
import com.marmorarias.platformadmin.application.AdminUserService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('platform_admin')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final PlatformAdminContext platformAdminContext;

    public AdminUserController(AdminUserService adminUserService, PlatformAdminContext platformAdminContext) {
        this.adminUserService = adminUserService;
        this.platformAdminContext = platformAdminContext;
    }

    public record ConvidarUsuarioRequest(UUID orgId, String email, String nome, Role role) {
    }

    public record AtualizarUsuarioRequest(Role role, String nome, Boolean ativo) {
    }

    @GetMapping
    public List<UserProfileEntity> listar(@RequestParam(required = false) UUID orgId) {
        return adminUserService.listar(orgId);
    }

    @PostMapping("/convites")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileEntity convidar(@RequestBody ConvidarUsuarioRequest request) {
        return adminUserService.convidar(platformAdminContext.userId(), request.orgId(), request.email(),
                request.nome(), request.role());
    }

    @PatchMapping("/{userId}")
    public UserProfileEntity atualizar(@PathVariable UUID userId, @RequestBody AtualizarUsuarioRequest request) {
        return adminUserService.atualizar(platformAdminContext.userId(), userId, request.role(), request.nome(),
                request.ativo());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID userId) {
        adminUserService.excluir(platformAdminContext.userId(), userId);
    }
}
