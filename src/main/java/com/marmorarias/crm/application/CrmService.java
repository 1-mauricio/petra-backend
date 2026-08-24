package com.marmorarias.crm.application;

import com.marmorarias.crm.adapter.persistence.CustomerEntity;
import com.marmorarias.crm.adapter.persistence.CustomerRepository;
import com.marmorarias.crm.adapter.persistence.LeadEntity;
import com.marmorarias.crm.adapter.persistence.LeadListItem;
import com.marmorarias.crm.adapter.persistence.LeadRepository;
import com.marmorarias.crm.domain.CustomerType;
import com.marmorarias.crm.domain.LeadStatus;
import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.identity.domain.TenantContext;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrmService {

    private final RlsContext rlsContext;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;

    public CrmService(RlsContext rlsContext, CustomerRepository customerRepository, LeadRepository leadRepository) {
        this.rlsContext = rlsContext;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
    }

    @Transactional
    public CustomerEntity criarCustomer(TenantContext tenant, CustomerType tipo, String nome, String cpfCnpj,
                                         String email, String telefone) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return customerRepository.save(new CustomerEntity(tenant.organizationId(), tipo, nome, cpfCnpj, email, telefone));
    }

    @Transactional(readOnly = true)
    public List<CustomerEntity> listarCustomers(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return customerRepository.findByOrganizationId(tenant.organizationId());
    }

    @Transactional(readOnly = true)
    public CustomerEntity buscarCustomer(TenantContext tenant, UUID customerId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Cliente não encontrado: " + customerId));
    }

    @Transactional
    public CustomerEntity atualizarCustomer(TenantContext tenant, UUID customerId, String nome, String cpfCnpj,
                                             String email, String telefone) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Cliente não encontrado: " + customerId));
        customer.atualizarDados(nome, cpfCnpj, email, telefone);
        return customer;
    }

    @Transactional
    public void deletarCustomer(TenantContext tenant, UUID customerId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        if (!customerRepository.existsById(customerId)) {
            throw new NoSuchElementException("Cliente não encontrado: " + customerId);
        }
        customerRepository.deleteById(customerId);
    }

    @Transactional
    public LeadEntity criarLead(TenantContext tenant, UUID customerId, String origem) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return leadRepository.save(new LeadEntity(tenant.organizationId(), customerId, origem));
    }

    @Transactional(readOnly = true)
    public List<LeadListItem> listarLeads(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return leadRepository.listarComCliente(tenant.organizationId());
    }

    /** Transição livre de status (ex.: NOVO -> CONTATADO -> QUALIFICADO); PERDIDO exige motivo (ver marcarLeadPerdido). */
    @Transactional
    public LeadEntity moverStatus(TenantContext tenant, UUID leadId, LeadStatus status, String motivoPerda) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        if (status == LeadStatus.PERDIDO) {
            return marcarLeadPerdido(tenant, leadId, motivoPerda);
        }
        LeadEntity lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new NoSuchElementException("Lead não encontrado: " + leadId));
        lead.marcarStatus(status);
        return lead;
    }

    @Transactional
    public LeadEntity marcarLeadConvertido(TenantContext tenant, UUID leadId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        LeadEntity lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new NoSuchElementException("Lead não encontrado: " + leadId));
        lead.marcarConvertido();
        return lead;
    }

    @Transactional
    public LeadEntity marcarLeadPerdido(TenantContext tenant, UUID leadId, String motivo) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        LeadEntity lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new NoSuchElementException("Lead não encontrado: " + leadId));
        lead.marcarPerdido(motivo);
        return lead;
    }
}
