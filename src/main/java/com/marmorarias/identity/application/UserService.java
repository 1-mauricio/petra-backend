package com.marmorarias.identity.application;

import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.identity.adapter.persistence.UserProfileEntity;
import com.marmorarias.identity.adapter.persistence.UserProfileRepository;
import com.marmorarias.identity.adapter.supabase.SupabaseAuthAdminClient;
import com.marmorarias.identity.domain.LimiteUsuariosExcedidoException;
import com.marmorarias.identity.domain.Role;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.platformbilling.application.BillingService;
import com.marmorarias.platformbilling.domain.PlanLimits;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final RlsContext rlsContext;
    private final UserProfileRepository userProfileRepository;
    private final SupabaseAuthAdminClient authAdminClient;
    private final BillingService billingService;

    public UserService(RlsContext rlsContext, UserProfileRepository userProfileRepository,
                        SupabaseAuthAdminClient authAdminClient, BillingService billingService) {
        this.rlsContext = rlsContext;
        this.userProfileRepository = userProfileRepository;
        this.authAdminClient = authAdminClient;
        this.billingService = billingService;
    }

    @Transactional(readOnly = true)
    public List<UserProfileEntity> listar(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return userProfileRepository.findByOrganizationId(tenant.organizationId());
    }

    /** Convite sempre pelo Supabase Auth (e-mail com link de definição de senha) — nunca senha local. */
    @Transactional
    public UserProfileEntity convidar(TenantContext tenant, String email, String nome, Role role) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        String plano = billingService.planoAtual(tenant.organizationId());
        long usuariosAtuais = userProfileRepository.countByOrganizationId(tenant.organizationId());
        if (!PlanLimits.usuarioDentroDoLimite(plano, usuariosAtuais)) {
            throw new LimiteUsuariosExcedidoException(
                    "Limite de usuários do plano " + plano + " atingido (" + PlanLimits.BASICO_MAX_USUARIOS + ")");
        }

        UUID authUserId = authAdminClient.convidar(email);
        return userProfileRepository.save(new UserProfileEntity(authUserId, tenant.organizationId(), role, nome, email));
    }
}
