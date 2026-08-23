package com.marmorarias.billing.adapter.web;

import com.marmorarias.billing.adapter.persistence.PaymentEntity;
import com.marmorarias.billing.adapter.persistence.ReceivableEntity;
import com.marmorarias.billing.adapter.persistence.ReceivableListItem;
import com.marmorarias.billing.application.BillingReceivableService;
import com.marmorarias.identity.adapter.security.CurrentTenant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReceivableController {

    private final BillingReceivableService billingReceivableService;
    private final CurrentTenant currentTenant;

    public ReceivableController(BillingReceivableService billingReceivableService, CurrentTenant currentTenant) {
        this.billingReceivableService = billingReceivableService;
        this.currentTenant = currentTenant;
    }

    public record CreateReceivableRequest(UUID orderId, BigDecimal valorTotal, int numeroParcelas,
                                           LocalDate primeiroVencimento) {
    }

    public record BaixaRequest(String formaPagamento) {
    }

    @GetMapping("/receivables")
    public List<ReceivableEntity> listar(@RequestParam UUID orderId) {
        return billingReceivableService.listarPorPedido(currentTenant.get(), orderId);
    }

    @GetMapping("/financeiro/recebiveis")
    public List<ReceivableListItem> listarRecebiveis() {
        return billingReceivableService.listar(currentTenant.get());
    }

    @PostMapping("/financeiro/parcelas/{id}/baixa")
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public PaymentEntity darBaixa(@PathVariable UUID id, @RequestBody BaixaRequest request) {
        return billingReceivableService.darBaixa(currentTenant.get(), id, request.formaPagamento());
    }

    @PostMapping("/receivables")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public ReceivableEntity criar(@RequestBody CreateReceivableRequest request) {
        return billingReceivableService.criar(currentTenant.get(), request.orderId(), request.valorTotal(),
                request.numeroParcelas(), request.primeiroVencimento());
    }
}
