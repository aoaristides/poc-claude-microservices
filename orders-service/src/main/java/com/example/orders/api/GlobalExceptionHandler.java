package com.example.orders.api;

import com.example.orders.domain.exception.InvalidOrderException;
import com.example.orders.domain.exception.InvalidTransitionException;
import com.example.orders.domain.exception.OrderNotFoundException;
import com.example.orders.domain.exception.SagaNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduz exceções de domínio em respostas HTTP padronizadas (RFC 7807 / ProblemDetail).
 * Exceção técnica não vaza para o cliente; cada erro de domínio tem seu status.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InvalidOrderException.class)
    ProblemDetail handleInvalidOrder(InvalidOrderException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setTitle("Pedido inválido");
        return pd;
    }

    @ExceptionHandler(InvalidTransitionException.class)
    ProblemDetail handleInvalidTransition(InvalidTransitionException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Transição de estado inválida");
        return pd;
    }

    @ExceptionHandler({OrderNotFoundException.class, SagaNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Recurso não encontrado");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Requisição inválida");
        return pd;
    }
}
