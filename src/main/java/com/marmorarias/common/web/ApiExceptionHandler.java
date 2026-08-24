package com.marmorarias.common.web;

import com.marmorarias.identity.domain.LimiteUsuariosExcedidoException;
import com.marmorarias.orders.domain.LimitePedidosExcedidoException;
import com.marmorarias.orders.domain.TransicaoInvalidaException;
import com.marmorarias.quoting.domain.DescontoExigeAdminException;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz exceções de domínio/persistência para respostas HTTP — nenhum controller lida com isso sozinho. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler({TransicaoInvalidaException.class, IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> requisicaoInvalida(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler({LimiteUsuariosExcedidoException.class, LimitePedidosExcedidoException.class})
    public ResponseEntity<Map<String, String>> limitePlanoExcedido(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(DescontoExigeAdminException.class)
    public ResponseEntity<Map<String, String>> descontoExigeAdmin(DescontoExigeAdminException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> violacaoDeIntegridade(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", "Dados inválidos ou fora das regras do banco"));
    }
}
