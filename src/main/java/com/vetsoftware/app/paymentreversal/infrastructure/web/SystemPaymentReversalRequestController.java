package com.vetsoftware.app.paymentreversal.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.paymentreversal.application.command.AcknowledgeReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.command.OpenPaymentReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.command.OpposeReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.command.ResolveReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.port.in.AcknowledgeReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.ListAllPaymentReversalRequestsUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.ListExpiringReversalRequestsUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.OpenPaymentReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.OpposeReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.ResolveReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.infrastructure.web.request.AcknowledgeReversalRequest;
import com.vetsoftware.app.paymentreversal.infrastructure.web.request.OpenReversalRequest;
import com.vetsoftware.app.paymentreversal.infrastructure.web.request.OpposeReversalRequest;
import com.vetsoftware.app.paymentreversal.infrastructure.web.request.ResolveReversalRequest;
import com.vetsoftware.app.paymentreversal.infrastructure.web.response.PaymentReversalRequestResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Instruccion de expedientes de reversion y barridos de plataforma.
 *
 * <p>
 * Todas las escrituras del bloque «Cobro y saldos» viven aqui, y todas van
 * cerradas a {@code hasRole('SYSTEM')} en su puerto. Es el caso de uso aparte
 * que el CLAUDE.md exige para lo que puede elegir company, nunca mezclado con
 * el camino de tenant.
 *
 * <p>
 * <strong>La empresa viaja como {@code @RequestParam}, jamas dentro del
 * cuerpo.</strong> Un principal SYSTEM no tiene empresa propia, asi que aqui
 * hay que elegirla; pero {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} es una regla
 * dura y <em>no admite excepcion para plataforma</em>: prohibe que cualquier
 * {@code @RequestBody} declare un {@code companyId}, porque un numero escrito
 * en el JSON convierte {@code @authz.isMyCompany(#command.companyId)} en una
 * comparacion consigo mismo. El query param es el camino declarado, y es el
 * mismo que usa {@code SystemSubscriptionPaymentController}.
 */
@RestController
@RequestMapping("/system/payment-reversal-requests")
public class SystemPaymentReversalRequestController {

    private final OpenPaymentReversalRequestUseCase openUseCase;
    private final AcknowledgeReversalRequestUseCase acknowledgeUseCase;
    private final OpposeReversalRequestUseCase opposeUseCase;
    private final ResolveReversalRequestUseCase resolveUseCase;
    private final ListExpiringReversalRequestsUseCase listExpiringUseCase;
    private final ListAllPaymentReversalRequestsUseCase listAllUseCase;

    public SystemPaymentReversalRequestController(OpenPaymentReversalRequestUseCase openUseCase,
            AcknowledgeReversalRequestUseCase acknowledgeUseCase,
            OpposeReversalRequestUseCase opposeUseCase,
            ResolveReversalRequestUseCase resolveUseCase,
            ListExpiringReversalRequestsUseCase listExpiringUseCase,
            ListAllPaymentReversalRequestsUseCase listAllUseCase) {
        this.openUseCase = openUseCase;
        this.acknowledgeUseCase = acknowledgeUseCase;
        this.opposeUseCase = opposeUseCase;
        this.resolveUseCase = resolveUseCase;
        this.listExpiringUseCase = listExpiringUseCase;
        this.listAllUseCase = listAllUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentReversalRequestResponse open(@RequestParam Long companyId,
            @Valid @RequestBody OpenReversalRequest request) {
        return PaymentReversalRequestResponse
                .from(openUseCase.execute(new OpenPaymentReversalRequestCommand(companyId,
                        request.paymentId(), request.origin(), request.causal(),
                        request.consumerDetermination(), request.consumerBecameAwareAt(),
                        request.claimReceivedAt(), request.issuerNotifiedAt(),
                        request.claimEvidenceRef(), request.deadlineAt())));
    }

    @PatchMapping("/{id}/acknowledgement")
    public PaymentReversalRequestResponse acknowledge(@PathVariable Long id,
            @RequestParam Long companyId, @Valid @RequestBody AcknowledgeReversalRequest request) {
        return PaymentReversalRequestResponse
                .from(acknowledgeUseCase.execute(new AcknowledgeReversalRequestCommand(id,
                        companyId, request.acknowledgementRef())));
    }

    @PatchMapping("/{id}/opposition")
    public PaymentReversalRequestResponse oppose(@PathVariable Long id,
            @RequestParam Long companyId, @Valid @RequestBody OpposeReversalRequest request) {
        return PaymentReversalRequestResponse
                .from(opposeUseCase.execute(new OpposeReversalRequestCommand(id, companyId,
                        request.ground(), request.oppositionEvidenceRef())));
    }

    @PatchMapping("/{id}/outcome")
    public PaymentReversalRequestResponse resolve(@PathVariable Long id,
            @RequestParam Long companyId, @Valid @RequestBody ResolveReversalRequest request) {
        return PaymentReversalRequestResponse
                .from(resolveUseCase.execute(new ResolveReversalRequestCommand(id, companyId,
                        request.outcome(), request.appliedAmount(), request.resultingRefundId())));
    }

    /**
     * El barrido de plazos: expedientes sin resolver cuyo plazo vence antes de
     * {@code before}. Recorre todas las clinicas a proposito.
     */
    @GetMapping("/expiring")
    public PageResponse<PaymentReversalRequestResponse> listExpiring(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listExpiringUseCase.listExpiring(before, page, pageSize),
                PaymentReversalRequestResponse::from);
    }

    @GetMapping
    public PageResponse<PaymentReversalRequestResponse> listAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listAllUseCase.listAll(companyId, page, pageSize),
                PaymentReversalRequestResponse::from);
    }
}
