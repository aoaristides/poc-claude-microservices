package com.example.shipping.api;

import com.example.shipping.domain.exception.DeliveryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz exceções de domínio para RFC 7807 (Problem Details).
 * O domínio não conhece HTTP; a tradução ocorre somente aqui.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DeliveryNotFoundException.class)
    ProblemDetail handleNotFound(DeliveryNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
