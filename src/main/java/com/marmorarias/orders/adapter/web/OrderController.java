package com.marmorarias.orders.adapter.web;

import com.marmorarias.identity.adapter.security.CurrentTenant;
import com.marmorarias.orders.adapter.persistence.CustomerOrderEntity;
import com.marmorarias.orders.adapter.persistence.CustomerOrderListItem;
import com.marmorarias.orders.application.OrderService;
import com.marmorarias.orders.application.OrderTransitionView;
import com.marmorarias.orders.domain.OrderState;
import com.marmorarias.quoting.application.CriarOrcamentoRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final CurrentTenant currentTenant;

    public OrderController(OrderService orderService, CurrentTenant currentTenant) {
        this.orderService = orderService;
        this.currentTenant = currentTenant;
    }

    public record CreateOrderRequest(UUID customerId, UUID quoteVersionId) {
    }

    public record CancelRequest(BigDecimal taxaMedicao, String motivo) {
    }

    public record NovaTransicaoRequest(OrderState toState) {
    }

    @GetMapping("/pedidos")
    public List<CustomerOrderListItem> listar(@RequestParam(required = false) OrderState estagio) {
        return orderService.listar(currentTenant.get(), estagio);
    }

    @GetMapping("/clientes/{customerId}/pedidos")
    public List<CustomerOrderListItem> listarPorCliente(@PathVariable UUID customerId) {
        return orderService.listarPorCliente(currentTenant.get(), customerId);
    }

    @GetMapping("/pedidos/{id}")
    public CustomerOrderListItem buscar(@PathVariable UUID id) {
        return orderService.buscarDetalhe(currentTenant.get(), id);
    }

    @GetMapping("/pedidos/{id}/transicoes")
    public List<OrderTransitionView> transicoesDisponiveis(@PathVariable UUID id) {
        return orderService.transicoesDisponiveis(currentTenant.get(), id);
    }

    @PostMapping("/pedidos/{id}/transicoes")
    @PreAuthorize("hasAnyRole('admin', 'comercial', 'producao')")
    public CustomerOrderEntity aplicarTransicao(@PathVariable UUID id, @RequestBody NovaTransicaoRequest request) {
        return orderService.transicionar(currentTenant.get(), id, request.toState(), null);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public CustomerOrderEntity criar(@RequestBody CreateOrderRequest request) {
        return orderService.criarPedido(currentTenant.get(), request.customerId(), request.quoteVersionId());
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public CustomerOrderEntity cancelar(@PathVariable UUID id, @RequestBody CancelRequest request) {
        return orderService.cancelar(currentTenant.get(), id, request.taxaMedicao(), request.motivo());
    }

    @PostMapping("/orders/{id}/revisar-orcamento")
    @PreAuthorize("hasAnyRole('admin', 'comercial')")
    public CustomerOrderEntity revisarOrcamento(@PathVariable UUID id, @RequestBody CriarOrcamentoRequest request) {
        return orderService.revisarOrcamento(currentTenant.get(), id, request);
    }
}
