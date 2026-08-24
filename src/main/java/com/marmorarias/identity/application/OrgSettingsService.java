package com.marmorarias.identity.application;

import com.marmorarias.identity.adapter.persistence.OrgSettingsEntity;
import com.marmorarias.identity.adapter.persistence.OrgSettingsRepository;
import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.identity.domain.TenantContext;
import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgSettingsService {

    private final RlsContext rlsContext;
    private final OrgSettingsRepository orgSettingsRepository;

    public OrgSettingsService(RlsContext rlsContext, OrgSettingsRepository orgSettingsRepository) {
        this.rlsContext = rlsContext;
        this.orgSettingsRepository = orgSettingsRepository;
    }

    @Transactional(readOnly = true)
    public OrgSettingsEntity buscar(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return orgSettingsRepository.findById(tenant.organizationId())
                .orElseThrow(() -> new NoSuchElementException("org_settings não encontrado"));
    }

    @Transactional
    public OrgSettingsEntity atualizar(TenantContext tenant, BigDecimal toleranciaPerc, BigDecimal toleranciaAbs,
                                        BigDecimal descontoLimitePerc, Map<String, BigDecimal> fatorPerda) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        OrgSettingsEntity settings = orgSettingsRepository.findById(tenant.organizationId())
                .orElseThrow(() -> new NoSuchElementException("org_settings não encontrado"));
        settings.atualizar(toleranciaPerc, toleranciaAbs, descontoLimitePerc, fatorPerda);
        return settings;
    }
}
