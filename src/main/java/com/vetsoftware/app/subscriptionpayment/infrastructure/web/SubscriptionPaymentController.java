package com.vetsoftware.app.subscriptionpayment.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.subscriptionpayment.application.command.ChangeSubscriptionPaymentStatusCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.ReconcileSubscriptionPaymentCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.RegisterSubscriptionPaymentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ChangeSubscriptionPaymentStatusUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.FindSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListSubscriptionPaymentsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ReconcileSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.RegisterSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.infrastructure.web.request.ChangeSubscriptionPaymentStatusRequest;
import com.vetsoftware.app.subscriptionpayment.infrastructure.web.request.RegisterSubscriptionPaymentRequest;
import com.vetsoftware.app.subscriptionpayment.infrastructure.web.response.SubscriptionPaymentResponse;
import jakarta.validation.Valid;
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
 * Moneda por defecto cuando el cliente no la manda. Existe una sola constante
 * aqui y no un literal repetido en tres metodos: el dia que se venda fuera de
 * Colombia, cambiar el defecto tiene que ser un sitio.
 */
@RestController
@RequestMapping("/subscription-payments")
public class SubscriptionPaymentController {

    private static final String DEFAULT_CURRENCY = "COP";

    private final RegisterSubscriptionPaymentUseCase registerUseCase;
    private final ChangeSubscriptionPaymentStatusUseCase changeStatusUseCase;
    private final ReconcileSubscriptionPaymentUseCase reconcileUseCase;
    private final FindSubscriptionPaymentUseCase findUseCase;
    private final ListSubscriptionPaymentsUseCase listUseCase;
    private final Authz authz;

    public SubscriptionPaymentController(RegisterSubscriptionPaymentUseCase registerUseCase,
            ChangeSubscriptionPaymentStatusUseCase changeStatusUseCase,
            ReconcileSubscriptionPaymentUseCase reconcileUseCase,
            FindSubscriptionPaymentUseCase findUseCase, ListSubscriptionPaymentsUseCase listUseCase,
            Authz authz) {
        this.registerUseCase = registerUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.reconcileUseCase = reconcileUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    /**
     * La empresa sale de {@code authz.currentCompanyId()} y nunca del cuerpo: es lo
     * que impide registrar un pago a nombre de otra clinica.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionPaymentResponse register(
            @Valid @RequestBody RegisterSubscriptionPaymentRequest request) {
        return toResponse(registerUseCase.execute(
                new RegisterSubscriptionPaymentCommand(authz.currentCompanyId(), request.amount(),
                        request.currency() == null ? DEFAULT_CURRENCY : request.currency(),
                        request.paymentMethod(), request.gateway(), request.gatewayReference(),
                        request.receivedAt(), request.clientRequestId())));
    }

    @GetMapping("/{id}")
    public SubscriptionPaymentResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @GetMapping
    public PageResponse<SubscriptionPaymentResponse> listByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                this::toResponse);
    }

    @PatchMapping("/{id}/status")
    public SubscriptionPaymentResponse changeStatus(@PathVariable Long id,
            @Valid @RequestBody ChangeSubscriptionPaymentStatusRequest request) {
        return toResponse(changeStatusUseCase.execute(new ChangeSubscriptionPaymentStatusCommand(id,
                authz.currentCompanyId(), request.status())));
    }

    /** Sin cuerpo: la fecha de conciliacion la pone el servidor, no el cliente. */
    @PatchMapping("/{id}/reconciliation")
    public SubscriptionPaymentResponse reconcile(@PathVariable Long id) {
        return toResponse(reconcileUseCase
                .execute(new ReconcileSubscriptionPaymentCommand(id, authz.currentCompanyId())));
    }

    private SubscriptionPaymentResponse toResponse(SubscriptionPaymentDto dto) {
        return new SubscriptionPaymentResponse(dto.id(), dto.companyId(), dto.amount(),
                dto.currency(), dto.paymentMethod(), dto.gateway(), dto.gatewayReference(),
                dto.receivedAt(), dto.status(), dto.reconciledAt(), dto.createdDate(),
                dto.version());
    }
}
