package com.example.inventory.api;

import com.example.inventory.domain.exception.StockItemNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz exceções de domínio para RFC 7807 (ProblemDetail).
 * Só é relevante para os endpoints REST de debug; a saga usa o outbox.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockItemNotFoundException.class)
    public ProblemDetail handleNotFound(StockItemNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Stock Item Not Found");
        return problem;
    }
}
