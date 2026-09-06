package com.vetsoftware.app.paymentgateway.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.paymentgateway.application.command.CreateWompiPaymentSourceCommand;
import com.vetsoftware.app.paymentgateway.application.port.in.CreateWompiPaymentSourceUseCase;
import com.vetsoftware.app.paymentgateway.application.port.in.FindFirstPeriodPaymentUseCase;
import com.vetsoftware.app.paymentgateway.application.port.in.GetWompiCheckoutConfigUseCase;
import com.vetsoftware.app.paymentgateway.infrastructure.web.request.WompiPaymentSourceRequest;
import com.vetsoftware.app.paymentgateway.infrastructure.web.response.FirstPeriodPaymentResponse;
import com.vetsoftware.app.paymentgateway.infrastructure.web.response.WompiCheckoutConfigResponse;
import com.vetsoftware.app.paymentgateway.infrastructure.web.response.WompiPaymentMethodResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El lado del tenant de la pasarela: la configuración para tokenizar, el alta
 * de la fuente de pago y el estado del cobro del primer periodo.
 *
 * <p>
 * La empresa nunca viaja en el cuerpo: sale de
 * {@code authz.currentCompanyId()}.
 */
@RestController
@RequestMapping("/payment-gateway/wompi")
public class WompiPaymentGatewayController {

    private final GetWompiCheckoutConfigUseCase checkoutConfigUseCase;
    private final CreateWompiPaymentSourceUseCase createPaymentSourceUseCase;
    private final FindFirstPeriodPaymentUseCase findFirstPeriodPaymentUseCase;
    private final Authz authz;

    public WompiPaymentGatewayController(GetWompiCheckoutConfigUseCase checkoutConfigUseCase,
            CreateWompiPaymentSourceUseCase createPaymentSourceUseCase,
            FindFirstPeriodPaymentUseCase findFirstPeriodPaymentUseCase, Authz authz) {
        this.checkoutConfigUseCase = checkoutConfigUseCase;
        this.createPaymentSourceUseCase = createPaymentSourceUseCase;
        this.findFirstPeriodPaymentUseCase = findFirstPeriodPaymentUseCase;
        this.authz = authz;
    }

    @GetMapping("/checkout-config")
    public WompiCheckoutConfigResponse checkoutConfig() {
        return WompiCheckoutConfigResponse.from(checkoutConfigUseCase.execute());
    }

    @PostMapping("/payment-sources")
    @ResponseStatus(HttpStatus.CREATED)
    public WompiPaymentMethodResponse createPaymentSource(
            @Valid @RequestBody WompiPaymentSourceRequest request) {
        return WompiPaymentMethodResponse.from(createPaymentSourceUseCase.execute(
                new CreateWompiPaymentSourceCommand(authz.currentCompanyId(), request.cardToken(),
                        request.acceptanceToken(), request.personalDataAuthToken(), request.brand(),
                        request.lastFour(), request.expMonth(), request.expYear())));
    }

    @GetMapping("/first-period-payment")
    public FirstPeriodPaymentResponse firstPeriodPayment() {
        return FirstPeriodPaymentResponse
                .from(findFirstPeriodPaymentUseCase.execute(authz.currentCompanyId()));
    }
}
