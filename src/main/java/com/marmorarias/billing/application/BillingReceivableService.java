package com.marmorarias.billing.application;

import com.marmorarias.billing.adapter.persistence.InstallmentEntity;
import com.marmorarias.billing.adapter.persistence.InstallmentRepository;
import com.marmorarias.billing.adapter.persistence.PaymentEntity;
import com.marmorarias.billing.adapter.persistence.PaymentRepository;
import com.marmorarias.billing.adapter.persistence.ReceivableEntity;
import com.marmorarias.billing.adapter.persistence.ReceivableListItem;
import com.marmorarias.billing.adapter.persistence.ReceivableRepository;
import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.identity.domain.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingReceivableService {

    private final RlsContext rlsContext;
    private final ReceivableRepository receivableRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentRepository paymentRepository;

    public BillingReceivableService(RlsContext rlsContext, ReceivableRepository receivableRepository,
                                     InstallmentRepository installmentRepository, PaymentRepository paymentRepository) {
        this.rlsContext = rlsContext;
        this.receivableRepository = receivableRepository;
        this.installmentRepository = installmentRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public ReceivableEntity criar(TenantContext tenant, UUID orderId, BigDecimal valorTotal, int numeroParcelas,
                                   LocalDate primeiroVencimento) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        ReceivableEntity receivable = receivableRepository.save(
                new ReceivableEntity(tenant.organizationId(), orderId, valorTotal));

        BigDecimal valorParcela = valorTotal.divide(BigDecimal.valueOf(numeroParcelas), 2, java.math.RoundingMode.HALF_UP);
        for (int i = 1; i <= numeroParcelas; i++) {
            installmentRepository.save(new InstallmentEntity(tenant.organizationId(), receivable.getId(), i,
                    valorParcela, primeiroVencimento.plusMonths(i - 1L)));
        }
        return receivable;
    }

    @Transactional(readOnly = true)
    public List<ReceivableEntity> listarPorPedido(TenantContext tenant, UUID orderId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return receivableRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<ReceivableListItem> listar(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        List<ReceivableListItem> resultado = new java.util.ArrayList<>();
        for (ReceivableEntity r : receivableRepository.findByOrganizationId(tenant.organizationId())) {
            List<ReceivableListItem.Parcela> parcelas = installmentRepository.findByReceivableId(r.getId()).stream()
                    .map(i -> new ReceivableListItem.Parcela(i.getId(), i.getReceivableId(), i.getNumero(),
                            i.getValor(), i.getVencimento(), i.getStatus()))
                    .toList();
            resultado.add(new ReceivableListItem(r.getId(), r.getOrderId(), r.getValorTotal(), parcelas));
        }
        return resultado;
    }

    @Transactional
    public PaymentEntity registrarPagamento(TenantContext tenant, UUID installmentId, BigDecimal valor,
                                             String formaPagamento) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        InstallmentEntity installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new NoSuchElementException("Parcela não encontrada: " + installmentId));
        installment.marcarPaga();
        return paymentRepository.save(
                new PaymentEntity(tenant.organizationId(), installmentId, valor, formaPagamento));
    }

    /** Baixa manual (tela Financeiro): a parcela já sabe o próprio valor, então basta a forma de pagamento. */
    @Transactional
    public PaymentEntity darBaixa(TenantContext tenant, UUID installmentId, String formaPagamento) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        InstallmentEntity installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new NoSuchElementException("Parcela não encontrada: " + installmentId));
        installment.marcarPaga();
        return paymentRepository.save(
                new PaymentEntity(tenant.organizationId(), installmentId, installment.getValor(), formaPagamento));
    }
}
