package com.vetsoftware.app.paymentreversal.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.paymentreversal.application.port.in.FindPaymentReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.ListPaymentReversalRequestsUseCase;
import com.vetsoftware.app.paymentreversal.infrastructure.web.response.PaymentReversalRequestResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara de tenant del expediente: <strong>solo lecturas</strong>.
 *
 * <p>
 * El bloque «Cobro y saldos» lo escribe la plataforma y lo leen ambos, asi que
 * aqui no hay ni un {@code POST} ni un {@code PATCH}. Instruir una reversion
 * —abrirla, acusarla, oponerse, resolverla— vive en
 * {@link SystemPaymentReversalRequestController}, y esa separacion es la que
 * impide que la clinica reclamante escriba en el expediente con el que la
 * plataforma se defiende.
 *
 * <p>
 * La empresa sale de {@code authz.currentCompanyId()} y nunca de la peticion.
 */
@RestController
@RequestMapping("/payment-reversal-requests")
public class PaymentReversalRequestController {

    private final FindPaymentReversalRequestUseCase findUseCase;
    private final ListPaymentReversalRequestsUseCase listUseCase;
    private final Authz authz;

    public PaymentReversalRequestController(FindPaymentReversalRequestUseCase findUseCase,
            ListPaymentReversalRequestsUseCase listUseCase, Authz authz) {
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @GetMapping("/{id}")
    public PaymentReversalRequestResponse findById(@PathVariable Long id) {
        return PaymentReversalRequestResponse
                .from(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @GetMapping
    public PageResponse<PaymentReversalRequestResponse> listByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                PaymentReversalRequestResponse::from);
    }
}
