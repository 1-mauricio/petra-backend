package com.marmorarias.identity.adapter.web;

import com.marmorarias.identity.adapter.persistence.UserProfileEntity;
import com.marmorarias.identity.adapter.security.CurrentTenant;
import com.marmorarias.identity.application.UserService;
import com.marmorarias.identity.domain.Role;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UserController {

    private final UserService userService;
    private final CurrentTenant currentTenant;

    public UserController(UserService userService, CurrentTenant currentTenant) {
        this.userService = userService;
        this.currentTenant = currentTenant;
    }

    public record ConvidarUsuarioRequest(String email, String nome, Role role) {
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public List<UserProfileEntity> listar() {
        return userService.listar(currentTenant.get());
    }

    @PostMapping("/convites")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    public UserProfileEntity convidar(@RequestBody ConvidarUsuarioRequest request) {
        return userService.convidar(currentTenant.get(), request.email(), request.nome(), request.role());
    }
}
