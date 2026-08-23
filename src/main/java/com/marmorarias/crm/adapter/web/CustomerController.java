package com.marmorarias.crm.adapter.web;

import com.marmorarias.crm.adapter.persistence.CustomerEntity;
import com.marmorarias.crm.application.CrmService;
import com.marmorarias.crm.domain.CustomerType;
import com.marmorarias.identity.adapter.security.CurrentTenant;
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
@RequestMapping("/api")
public class CustomerController {

    private final CrmService crmService;
    private final CurrentTenant currentTenant;

    public CustomerController(CrmService crmService, CurrentTenant currentTenant) {
        this.crmService = crmService;
        this.currentTenant = currentTenant;
    }

    public record CreateCustomerRequest(CustomerType tipo, String nome, String cpfCnpj, String email, String telefone) {
    }

    public record AtualizarCustomerRequest(String nome, String cpfCnpj, String email, String telefone) {
    }

    @GetMapping("/clientes")
    public List<CustomerEntity> listar() {
        return crmService.listarCustomers(currentTenant.get());
    }

    @GetMapping("/clientes/{id}")
    public CustomerEntity buscar(@PathVariable UUID id) {
        return crmService.buscarCustomer(currentTenant.get(), id);
    }

    @PostMapping("/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public CustomerEntity criar(@RequestBody CreateCustomerRequest request) {
        return crmService.criarCustomer(currentTenant.get(), request.tipo(), request.nome(), request.cpfCnpj(),
                request.email(), request.telefone());
    }

    @PatchMapping("/clientes/{id}")
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public CustomerEntity atualizar(@PathVariable UUID id, @RequestBody AtualizarCustomerRequest request) {
        return crmService.atualizarCustomer(currentTenant.get(), id, request.nome(), request.cpfCnpj(),
                request.email(), request.telefone());
    }

    @DeleteMapping("/clientes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public void deletar(@PathVariable UUID id) {
        crmService.deletarCustomer(currentTenant.get(), id);
    }
}
