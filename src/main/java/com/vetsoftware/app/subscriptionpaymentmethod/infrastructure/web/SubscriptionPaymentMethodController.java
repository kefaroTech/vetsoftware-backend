package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.subscriptionpaymentmethod.application.command.RegisterSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.command.RevokeSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.command.SetDefaultPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.FindSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ListSubscriptionPaymentMethodsUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.RegisterSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.RevokeSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.SetDefaultPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.web.request.RegisterSubscriptionPaymentMethodRequest;
import com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.web.request.RevokeSubscriptionPaymentMethodRequest;
import com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.web.response.SubscriptionPaymentMethodResponse;
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
 * Los medios de pago de la clinica.
 *
 * <p>
 * <strong>Este controller si expone escrituras del tenant</strong>, a
 * diferencia del resto del circuito de cobro. La tarjeta pertenece al bloque
 * <em>Facturacion del cliente</em> del documento maestro —«son datos de la
 * clinica»—, no al de <em>Cobro y saldos</em>: el cliente registra la suya,
 * elige cual es la predeterminada y, sobre todo, <strong>revoca</strong>, que
 * es un derecho que no puede quedar detras de una gestion de plataforma.
 *
 * <p>
 * La empresa nunca viaja en el cuerpo: sale de
 * {@code authz.currentCompanyId()}.
 */
@RestController
@RequestMapping("/subscription-payment-methods")
public class SubscriptionPaymentMethodController {

    private final RegisterSubscriptionPaymentMethodUseCase registerUseCase;
    private final RevokeSubscriptionPaymentMethodUseCase revokeUseCase;
    private final SetDefaultPaymentMethodUseCase setDefaultUseCase;
    private final FindSubscriptionPaymentMethodUseCase findUseCase;
    private final ListSubscriptionPaymentMethodsUseCase listUseCase;
    private final Authz authz;

    public SubscriptionPaymentMethodController(
            RegisterSubscriptionPaymentMethodUseCase registerUseCase,
            RevokeSubscriptionPaymentMethodUseCase revokeUseCase,
            SetDefaultPaymentMethodUseCase setDefaultUseCase,
            FindSubscriptionPaymentMethodUseCase findUseCase,
            ListSubscriptionPaymentMethodsUseCase listUseCase, Authz authz) {
        this.registerUseCase = registerUseCase;
        this.revokeUseCase = revokeUseCase;
        this.setDefaultUseCase = setDefaultUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionPaymentMethodResponse register(
            @Valid @RequestBody RegisterSubscriptionPaymentMethodRequest request) {
        return SubscriptionPaymentMethodResponse
                .from(registerUseCase.execute(new RegisterSubscriptionPaymentMethodCommand(
                        authz.currentCompanyId(), request.methodKind(), request.gateway(),
                        request.token(), request.brand(), request.lastFour(), request.expiresOn(),
                        request.mandateEvidence(), request.authorizedAt())));
    }

    /** Fin del mandato. La fecha la pone el servidor, no el cuerpo. */
    @PatchMapping("/{id}/revocation")
    public SubscriptionPaymentMethodResponse revoke(@PathVariable Long id,
            @Valid @RequestBody RevokeSubscriptionPaymentMethodRequest request) {
        return SubscriptionPaymentMethodResponse
                .from(revokeUseCase.execute(new RevokeSubscriptionPaymentMethodCommand(id,
                        authz.currentCompanyId(), request.reason())));
    }

    /** Sin cuerpo: el recurso que se marca ya lo identifica la ruta. */
    @PatchMapping("/{id}/default")
    public SubscriptionPaymentMethodResponse setDefault(@PathVariable Long id) {
        return SubscriptionPaymentMethodResponse.from(setDefaultUseCase
                .execute(new SetDefaultPaymentMethodCommand(id, authz.currentCompanyId())));
    }

    @GetMapping("/{id}")
    public SubscriptionPaymentMethodResponse findById(@PathVariable Long id) {
        return SubscriptionPaymentMethodResponse
                .from(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @GetMapping
    public PageResponse<SubscriptionPaymentMethodResponse> listByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                SubscriptionPaymentMethodResponse::from);
    }
}
