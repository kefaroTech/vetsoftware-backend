package com.vetsoftware.app.paymentattempt.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.paymentattempt.application.port.in.FindPaymentAttemptUseCase;
import com.vetsoftware.app.paymentattempt.application.port.in.ListPaymentAttemptsByDocumentUseCase;
import com.vetsoftware.app.paymentattempt.application.port.in.ListPaymentAttemptsUseCase;
import com.vetsoftware.app.paymentattempt.infrastructure.web.response.PaymentAttemptResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara de tenant de {@code payment_attempts}: <strong>solo
 * lecturas</strong>.
 *
 * <p>
 * El bloque <em>Cobro y saldos</em> reparte asi la tabla — escribe plataforma,
 * leen ambos—, asi que registrar y reprogramar viven en
 * {@link SystemPaymentAttemptController} y aqui no hay ni un
 * {@code @PostMapping}. Todo lo que sale de aqui es
 * {@code PaymentAttemptResponse}, que no lleva el codigo crudo de la pasarela.
 *
 * <p>
 * La empresa sale siempre de {@code authz.currentCompanyId()} y nunca de la URL
 * ni del cuerpo.
 */
@RestController
@RequestMapping("/payment-attempts")
public class PaymentAttemptController {

    private final FindPaymentAttemptUseCase findUseCase;
    private final ListPaymentAttemptsUseCase listUseCase;
    private final ListPaymentAttemptsByDocumentUseCase listByDocumentUseCase;
    private final Authz authz;

    public PaymentAttemptController(FindPaymentAttemptUseCase findUseCase,
            ListPaymentAttemptsUseCase listUseCase,
            ListPaymentAttemptsByDocumentUseCase listByDocumentUseCase, Authz authz) {
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByDocumentUseCase = listByDocumentUseCase;
        this.authz = authz;
    }

    @GetMapping("/{id}")
    public PaymentAttemptResponse findById(@PathVariable Long id) {
        return PaymentAttemptResponse.from(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @GetMapping
    public PageResponse<PaymentAttemptResponse> listByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                PaymentAttemptResponse::from);
    }

    /** El historial de una factura: lo que sostiene "se intento cuatro veces". */
    @GetMapping("/by-document/{billingDocumentId}")
    public PageResponse<PaymentAttemptResponse> listByDocument(@PathVariable Long billingDocumentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listByDocumentUseCase.listByDocumentAndCompany(billingDocumentId,
                authz.currentCompanyId(), page, pageSize), PaymentAttemptResponse::from);
    }
}
