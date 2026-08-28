package com.vetsoftware.app.paymentattempt.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.paymentattempt.application.command.RecordPaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.command.ReschedulePaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.port.in.ListAllPaymentAttemptsUseCase;
import com.vetsoftware.app.paymentattempt.application.port.in.ListDuePaymentAttemptsUseCase;
import com.vetsoftware.app.paymentattempt.application.port.in.RecordPaymentAttemptUseCase;
import com.vetsoftware.app.paymentattempt.application.port.in.ReschedulePaymentAttemptUseCase;
import com.vetsoftware.app.paymentattempt.infrastructure.web.request.RecordPaymentAttemptRequest;
import com.vetsoftware.app.paymentattempt.infrastructure.web.request.ReschedulePaymentAttemptRequest;
import com.vetsoftware.app.paymentattempt.infrastructure.web.response.SystemPaymentAttemptResponse;
import jakarta.validation.Valid;
import java.time.Clock;
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
 * Cobranza de plataforma: la escritura de {@code payment_attempts} y los dos
 * listados cross-tenant. Todo lo que sirve es
 * {@link SystemPaymentAttemptResponse}, que si publica el codigo crudo de la
 * pasarela.
 */
@RestController
@RequestMapping("/system/payment-attempts")
public class SystemPaymentAttemptController {

    private final RecordPaymentAttemptUseCase recordUseCase;
    private final ReschedulePaymentAttemptUseCase rescheduleUseCase;
    private final ListDuePaymentAttemptsUseCase listDueUseCase;
    private final ListAllPaymentAttemptsUseCase listAllUseCase;
    private final Clock clock;

    public SystemPaymentAttemptController(RecordPaymentAttemptUseCase recordUseCase,
            ReschedulePaymentAttemptUseCase rescheduleUseCase,
            ListDuePaymentAttemptsUseCase listDueUseCase,
            ListAllPaymentAttemptsUseCase listAllUseCase, Clock clock) {
        this.recordUseCase = recordUseCase;
        this.rescheduleUseCase = rescheduleUseCase;
        this.listDueUseCase = listDueUseCase;
        this.listAllUseCase = listAllUseCase;
        this.clock = clock;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemPaymentAttemptResponse record(@RequestParam Long companyId,
            @Valid @RequestBody RecordPaymentAttemptRequest request) {
        return SystemPaymentAttemptResponse
                .from(recordUseCase.execute(new RecordPaymentAttemptCommand(companyId,
                        request.billingDocumentId(), request.paymentMethodId(), request.gateway(),
                        request.requestedAmount(), request.gatewayDeclineCode(),
                        request.declineKind(), request.attemptedAt(), request.nextAttemptAt())));
    }

    @PatchMapping("/{id}/schedule")
    public SystemPaymentAttemptResponse reschedule(@PathVariable Long id,
            @RequestParam Long companyId,
            @Valid @RequestBody ReschedulePaymentAttemptRequest request) {
        return SystemPaymentAttemptResponse.from(rescheduleUseCase.execute(
                new ReschedulePaymentAttemptCommand(id, companyId, request.nextAttemptAt())));
    }

    /**
     * La cola de reintentos de todas las clinicas.
     *
     * <p>
     * {@code dueBefore} se puede fijar para poder reproducir un barrido; cuando no
     * viene lo pone el <strong>reloj inyectado</strong>, nunca un
     * {@code LocalDateTime.now()} pelado.
     */
    @GetMapping("/due")
    public PageResponse<SystemPaymentAttemptResponse> listDue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        LocalDateTime cutoff = dueBefore == null ? LocalDateTime.now(clock) : dueBefore;
        return PageResponse.from(listDueUseCase.listDue(cutoff, page, pageSize),
                SystemPaymentAttemptResponse::from);
    }

    @GetMapping
    public PageResponse<SystemPaymentAttemptResponse> listAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listAllUseCase.listAll(companyId, page, pageSize),
                SystemPaymentAttemptResponse::from);
    }
}
