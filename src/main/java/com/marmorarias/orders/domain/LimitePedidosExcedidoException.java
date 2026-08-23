package com.marmorarias.orders.domain;

public class LimitePedidosExcedidoException extends RuntimeException {

    public LimitePedidosExcedidoException(String message) {
        super(message);
    }
}
