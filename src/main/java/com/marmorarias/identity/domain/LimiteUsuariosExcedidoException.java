package com.marmorarias.identity.domain;

public class LimiteUsuariosExcedidoException extends RuntimeException {

    public LimiteUsuariosExcedidoException(String message) {
        super(message);
    }
}
