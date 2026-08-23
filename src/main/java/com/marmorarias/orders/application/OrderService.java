package com.marmorarias.orders.application;

import com.marmorarias.identity.adapter.persistence.RlsContext;
import com.marmorarias.identity.domain.TenantContext;
import com.marmorarias.measurement.adapter.persistence.MeasurementRepository;
import com.marmorarias.measurement.domain.MeasurementStatus;
import com.marmorarias.orders.adapter.persistence.AuditLogEntity;
import com.marmorarias.orders.adapter.persistence.AuditLogRepository;
import com.marmorarias.orders.adapter.persistence.CustomerOrderEntity;
import com.marmorarias.orders.adapter.persistence.CustomerOrderListItem;
import com.marmorarias.orders.adapter.persistence.CustomerOrderRepository;
import com.marmorarias.orders.adapter.persistence.StageTransitionEntity;
import com.marmorarias.orders.adapter.persistence.StageTransitionRepository;
import com.marmorarias.orders.domain.OrderState;
import com.marmorarias.orders.domain.OrderStateMachine;
import com.marmorarias.orders.domain.TransicaoInvalidaException;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionEntity;
import com.marmorarias.quoting.adapter.persistence.QuoteVersionRepository;
import com.marmorarias.quoting.application.CriarOrcamentoRequest;
import com.marmorarias.quoting.application.QuoteService;
import com.marmorarias.quoting.domain.QuoteVersionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestrador central do pedido. Toda mudança de estágio passa por aqui: valida a topologia do
 * pipeline (OrderStateMachine), aplica o guard do invariante 1 quando o alvo é PRODUCAO, grava
 * stage_transition (append-only) e audit_log. Nunca expõe um PATCH de status solto.
 */
@Service
public class OrderService {

    private final RlsContext rlsContext;
    private final CustomerOrderRepository customerOrderRepository;
    private final StageTransitionRepository stageTransitionRepository;
    private final AuditLogRepository auditLogRepository;
    private final QuoteVersionRepository quoteVersionRepository;
    private final MeasurementRepository measurementRepository;
    private final QuoteService quoteService;

    public OrderService(RlsContext rlsContext, CustomerOrderRepository customerOrderRepository,
                         StageTransitionRepository stageTransitionRepository, AuditLogRepository auditLogRepository,
                         QuoteVersionRepository quoteVersionRepository, MeasurementRepository measurementRepository,
                         QuoteService quoteService) {
        this.rlsContext = rlsContext;
        this.customerOrderRepository = customerOrderRepository;
        this.stageTransitionRepository = stageTransitionRepository;
        this.auditLogRepository = auditLogRepository;
        this.quoteVersionRepository = quoteVersionRepository;
        this.measurementRepository = measurementRepository;
        this.quoteService = quoteService;
    }

    /**
     * Cria a nova quote_version (v+1, via QuoteService) e reaponta o pedido para ela — depois disso
     * a versão anterior aprovada fica read-only e PRODUCAO fica bloqueado até essa nova versão ser
     * reenviada e reaprovada pelo cliente (guard do invariante 1 relê sempre a versão corrente).
     */
    @Transactional
    public CustomerOrderEntity revisarOrcamento(TenantContext tenant, UUID orderId, CriarOrcamentoRequest request) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        CustomerOrderEntity order = buscarOrder(orderId);
        QuoteVersionEntity versaoAtual = quoteVersionRepository.findById(order.getCurrentQuoteVersionId())
                .orElseThrow(() -> new NoSuchElementException("Orçamento corrente não encontrado"));
        QuoteVersionEntity novaVersao = quoteService.revisar(tenant, versaoAtual.getQuoteId(), request);
        order.apontarNovaVersao(novaVersao.getId());
        registrarAuditoria(tenant, order.getId(), "REVISAO_ORCAMENTO_NOVA_VERSAO");
        return order;
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderListItem> listar(TenantContext tenant) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return customerOrderRepository.listarComCliente(tenant.organizationId());
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderListItem> listar(TenantContext tenant, OrderState estagio) {
        List<CustomerOrderListItem> todos = listar(tenant);
        return estagio == null ? todos : todos.stream().filter(o -> o.state() == estagio).toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderListItem> listarPorCliente(TenantContext tenant, UUID customerId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return customerOrderRepository.listarPorCliente(customerId);
    }

    @Transactional
    public CustomerOrderEntity criarPedido(TenantContext tenant, UUID customerId, UUID quoteVersionId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        CustomerOrderEntity order = new CustomerOrderEntity(tenant.organizationId(), customerId, quoteVersionId);
        order = customerOrderRepository.save(order);
        registrarAuditoria(tenant, order.getId(), "CRIACAO");
        return order;
    }

    @Transactional
    public CustomerOrderEntity transicionar(TenantContext tenant, UUID orderId, OrderState target, String motivo) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        CustomerOrderEntity order = buscarOrder(orderId);
        aplicarTransicao(tenant, order, target, motivo);
        return order;
    }

    @Transactional
    public CustomerOrderEntity cancelar(TenantContext tenant, UUID orderId, BigDecimal taxaMedicao, String motivo) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        CustomerOrderEntity order = buscarOrder(orderId);
        order.registrarCancelamento(taxaMedicao, motivo);
        aplicarTransicao(tenant, order, OrderState.CANCELADO, motivo);
        return order;
    }

    /**
     * Chamado pelo fluxo de medição quando a divergência excede a tolerância (invariante 2). Cria
     * a nova quote_version (não aprovada) a partir das medidas reais e só então transiciona para
     * REVISAO_ORCAMENTO — se só transicionasse o estágio sem trocar a versão corrente, o guard de
     * PRODUCAO (invariante 1) veria a versão antiga ainda como APROVADO e liberaria por engano.
     */
    @Transactional
    public CustomerOrderEntity forcarRevisaoOrcamento(TenantContext tenant, UUID orderId,
                                                        CriarOrcamentoRequest novoOrcamento, String motivo) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        CustomerOrderEntity order = buscarOrder(orderId);
        QuoteVersionEntity versaoAtual = quoteVersionRepository.findById(order.getCurrentQuoteVersionId())
                .orElseThrow(() -> new NoSuchElementException("Orçamento corrente não encontrado"));
        QuoteVersionEntity novaVersao = quoteService.revisar(tenant, versaoAtual.getQuoteId(), novoOrcamento);
        order.apontarNovaVersao(novaVersao.getId());

        aplicarTransicao(tenant, order, OrderState.REVISAO_ORCAMENTO, motivo);
        return order;
    }

    private void aplicarTransicao(TenantContext tenant, CustomerOrderEntity order, OrderState target, String motivo) {
        OrderState origem = order.getState();
        if (!OrderStateMachine.permitida(origem, target)) {
            throw new TransicaoInvalidaException("Transição inválida: " + origem + " -> " + target);
        }
        if (target == OrderState.PRODUCAO) {
            String motivoBloqueio = motivoBloqueioProducao(order);
            if (motivoBloqueio != null) {
                throw new TransicaoInvalidaException(motivoBloqueio);
            }
        }

        order.aplicarTransicao(target);
        stageTransitionRepository.save(
                new StageTransitionEntity(tenant.organizationId(), order.getId(), origem, target, tenant.userId(), motivo));
        registrarAuditoria(tenant, order.getId(), "TRANSICAO_" + target);
    }

    /** Invariante 1: só alcança PRODUCAO com measurement APROVADO e sem revisão de orçamento pendente. */
    private String motivoBloqueioProducao(CustomerOrderEntity order) {
        if (!measurementRepository.existsByOrderIdAndStatus(order.getId(), MeasurementStatus.APROVADO)) {
            return "Nenhuma medição aprovada para este pedido";
        }
        QuoteVersionEntity quoteVersion = quoteVersionRepository.findById(order.getCurrentQuoteVersionId())
                .orElseThrow(() -> new NoSuchElementException("Orçamento corrente não encontrado"));
        if (quoteVersion.getStatus() != QuoteVersionStatus.APROVADO) {
            return "Revisão de orçamento pendente";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public CustomerOrderListItem buscarDetalhe(TenantContext tenant, UUID orderId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        return customerOrderRepository.buscarComClientePorId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderTransitionView> transicoesDisponiveis(TenantContext tenant, UUID orderId) {
        rlsContext.setCurrentOrg(tenant.organizationId());
        CustomerOrderEntity order = buscarOrder(orderId);
        List<OrderTransitionView> views = new java.util.ArrayList<>();
        for (OrderState destino : OrderStateMachine.proximosEstados(order.getState())) {
            String motivoBloqueio = destino == OrderState.PRODUCAO ? motivoBloqueioProducao(order) : null;
            views.add(new OrderTransitionView(destino, motivoBloqueio == null, motivoBloqueio));
        }
        return views;
    }

    private CustomerOrderEntity buscarOrder(UUID orderId) {
        return customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado: " + orderId));
    }

    private void registrarAuditoria(TenantContext tenant, UUID orderId, String acao) {
        auditLogRepository.save(
                new AuditLogEntity(tenant.organizationId(), tenant.userId(), "customer_order", orderId, acao));
    }
}
