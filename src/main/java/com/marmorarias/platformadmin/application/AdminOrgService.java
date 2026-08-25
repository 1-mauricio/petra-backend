package com.marmorarias.platformadmin.application;

import com.marmorarias.identity.adapter.persistence.OrgSettingsRepository;
import com.marmorarias.identity.adapter.persistence.PlatformRlsContext;
import com.marmorarias.identity.adapter.persistence.UserProfileRepository;
import com.marmorarias.platformadmin.adapter.persistence.OrganizationEntity;
import com.marmorarias.platformadmin.adapter.persistence.OrganizationRepository;
import com.marmorarias.platformbilling.adapter.persistence.SubscriptionRepository;
import com.marmorarias.platformbilling.domain.OrgBillingStatus;
import com.marmorarias.platformbilling.domain.PlanLimits;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de organizações para o admin da plataforma. Mutações vão pra log estruturado (não pra
 * audit_log — actor lá referencia user_profile, e o admin não tem linha ali por não ter org;
 * ver PlatformAdminContext).
 */
@Service
public class AdminOrgService {

    private static final Logger log = LoggerFactory.getLogger(AdminOrgService.class);

    private final PlatformRlsContext platformRlsContext;
    private final OrganizationRepository organizationRepository;
    private final UserProfileRepository userProfileRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrgSettingsRepository orgSettingsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminOrgService(PlatformRlsContext platformRlsContext, OrganizationRepository organizationRepository,
                            UserProfileRepository userProfileRepository,
                            SubscriptionRepository subscriptionRepository,
                            OrgSettingsRepository orgSettingsRepository) {
        this.platformRlsContext = platformRlsContext;
        this.organizationRepository = organizationRepository;
        this.userProfileRepository = userProfileRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orgSettingsRepository = orgSettingsRepository;
    }

    public record OrgSummary(UUID id, String nome, String cnpj, OrgBillingStatus billingStatus, String plano,
                              long usuarios, Instant createdAt) {
    }

    @Transactional(readOnly = true)
    public List<OrgSummary> listar() {
        platformRlsContext.enablePlatformScope();
        return organizationRepository.findAll().stream().map(this::resumo).toList();
    }

    @Transactional(readOnly = true)
    public OrgSummary buscar(UUID orgId) {
        platformRlsContext.enablePlatformScope();
        return resumo(buscarEntidade(orgId));
    }

    private OrganizationEntity buscarEntidade(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organização não encontrada: " + orgId));
    }

    private OrgSummary resumo(OrganizationEntity org) {
        long usuarios = userProfileRepository.countByOrganizationId(org.getId());
        String plano = subscriptionRepository.findByOrganizationId(org.getId())
                .map(sub -> sub.getPlano())
                .orElse(PlanLimits.PLANO_BASICO);
        return new OrgSummary(org.getId(), org.getNome(), org.getCnpj(), org.getBillingStatus(), plano, usuarios,
                org.getCreatedAt());
    }

    /** Cria organização + org_settings (mesma dupla de V12/V19 — todo módulo de settings exige a linha). */
    @Transactional
    public OrganizationEntity criar(UUID adminUserId, String nome, String cnpj) {
        platformRlsContext.enablePlatformScope();
        OrganizationEntity org = organizationRepository.save(new OrganizationEntity(nome, cnpj));
        entityManager.createNativeQuery("INSERT INTO org_settings (organization_id) VALUES (?1)")
                .setParameter(1, org.getId())
                .executeUpdate();
        log.info("platform_admin={} criou organizacao={} nome={}", adminUserId, org.getId(), nome);
        return org;
    }

    @Transactional
    public OrganizationEntity atualizar(UUID adminUserId, UUID orgId, String nome, String cnpj,
                                         OrgBillingStatus billingStatus) {
        platformRlsContext.enablePlatformScope();
        OrganizationEntity org = buscarEntidade(orgId);
        org.atualizar(nome, cnpj, billingStatus);
        log.info("platform_admin={} atualizou organizacao={}", adminUserId, orgId);
        return org;
    }

    /**
     * org_settings existe pra toda organização (criada junto em criar()) e não tem
     * ON DELETE CASCADE, então precisa ser removida explicitamente antes. Além dela, toda
     * tabela de domínio referencia organization sem cascade (schema de propósito — ver V2-V10):
     * o FK barra a exclusão de qualquer org que já tenha cliente/pedido/usuário etc. Não
     * tentamos contornar isso — só funciona pra org vazia.
     */
    @Transactional
    public void excluir(UUID adminUserId, UUID orgId) {
        platformRlsContext.enablePlatformScope();
        OrganizationEntity org = buscarEntidade(orgId);
        orgSettingsRepository.deleteById(orgId);
        organizationRepository.delete(org);
        log.info("platform_admin={} excluiu organizacao={}", adminUserId, orgId);
    }
}
