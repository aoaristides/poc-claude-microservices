package com.example.payment.api;

import com.example.payment.domain.exception.InvalidPaymentTransitionException;
import com.example.payment.domain.exception.PaymentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz exceções de domínio para RFC 7807 (Problem Details).
 * Isola detalhes de infra da resposta HTTP.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    ProblemDetail handleNotFound(PaymentNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentTransitionException.class)
    ProblemDetail handleInvalidTransition(InvalidPaymentTransitionException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
