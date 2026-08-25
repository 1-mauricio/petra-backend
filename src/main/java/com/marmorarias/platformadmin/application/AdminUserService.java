package com.marmorarias.platformadmin.application;

import com.marmorarias.identity.adapter.persistence.PlatformRlsContext;
import com.marmorarias.identity.adapter.persistence.UserProfileEntity;
import com.marmorarias.identity.adapter.persistence.UserProfileRepository;
import com.marmorarias.identity.application.UserService;
import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de usuários entre organizações para o admin da plataforma. */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final PlatformRlsContext platformRlsContext;
    private final UserProfileRepository userProfileRepository;
    private final UserService userService;

    public AdminUserService(PlatformRlsContext platformRlsContext, UserProfileRepository userProfileRepository,
                             UserService userService) {
        this.platformRlsContext = platformRlsContext;
        this.userProfileRepository = userProfileRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<UserProfileEntity> listar(UUID orgId) {
        platformRlsContext.enablePlatformScope();
        return orgId != null ? userProfileRepository.findByOrganizationId(orgId) : userProfileRepository.findAll();
    }

    /**
     * Convite passa pelo mesmo caminho de UserService (Supabase Auth + limite de plano da org
     * alvo) — o admin não pula a regra de negócio, só age em nome de uma org que não é a dele.
     */
    @Transactional
    public UserProfileEntity convidar(UUID adminUserId, UUID orgId, String email, String nome, Role role) {
        UserProfileEntity criado = userService.convidar(new TenantContext(orgId, adminUserId, Role.admin), email, nome, role);
        log.info("platform_admin={} convidou usuario={} org={}", adminUserId, criado.getId(), orgId);
        return criado;
    }

    @Transactional
    public UserProfileEntity atualizar(UUID adminUserId, UUID userId, Role role, String nome, Boolean ativo) {
        platformRlsContext.enablePlatformScope();
        UserProfileEntity user = buscar(userId);
        user.atualizar(role, nome, ativo);
        log.info("platform_admin={} atualizou usuario={}", adminUserId, userId);
        return user;
    }

    @Transactional
    public void excluir(UUID adminUserId, UUID userId) {
        platformRlsContext.enablePlatformScope();
        userProfileRepository.delete(buscar(userId));
        log.info("platform_admin={} excluiu usuario={}", adminUserId, userId);
    }

    private UserProfileEntity buscar(UUID userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado: " + userId));
    }
}
