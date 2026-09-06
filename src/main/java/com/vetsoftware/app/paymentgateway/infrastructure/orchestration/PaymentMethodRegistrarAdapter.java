package com.vetsoftware.app.paymentgateway.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentgateway.application.dto.WompiPaymentMethodDto;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentMethodRegistrarPort;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.subscriptionpaymentmethod.application.command.RegisterSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.command.SetDefaultPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.RegisterSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.SetDefaultPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodKind;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Delega en {@code subscriptionpaymentmethod} el alta del mandato.
 *
 * <p>
 * <strong>Con {@code SystemAuthRunner}</strong>:
 * {@code SetDefaultPaymentMethodUseCase} exige
 * {@code subscriptionPaymentMethod.update}, que es un permiso distinto del
 * {@code .create} que ya validó {@code CreateWompiPaymentSourceUseCase}. Sin
 * escalar, marcar la tarjeta recién dada de alta como predeterminada dependería
 * de que el rol del empleado tuviera además ese segundo permiso —un
 * acoplamiento que este adaptador no debe imponer.
 */
@Component
public class PaymentMethodRegistrarAdapter implements PaymentMethodRegistrarPort {

    private final RegisterSubscriptionPaymentMethodUseCase registerUseCase;
    private final SetDefaultPaymentMethodUseCase setDefaultUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public PaymentMethodRegistrarAdapter(RegisterSubscriptionPaymentMethodUseCase registerUseCase,
            SetDefaultPaymentMethodUseCase setDefaultUseCase, SystemAuthRunner systemAuthRunner) {
        this.registerUseCase = registerUseCase;
        this.setDefaultUseCase = setDefaultUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public WompiPaymentMethodDto registerDefaultCard(Long companyId, String token, String brand,
            String lastFour, LocalDate expiresOn, String mandateEvidence,
            LocalDateTime authorizedAt) {
        SubscriptionPaymentMethodDto registered = systemAuthRunner
                .call(() -> registerUseCase.execute(new RegisterSubscriptionPaymentMethodCommand(
                        companyId, PaymentMethodKind.CARD, PaymentGatewayNames.WOMPI, token, brand,
                        lastFour, expiresOn, mandateEvidence, authorizedAt)));
        SubscriptionPaymentMethodDto defaulted = systemAuthRunner.call(() -> setDefaultUseCase
                .execute(new SetDefaultPaymentMethodCommand(registered.id(), companyId)));
        return new WompiPaymentMethodDto(defaulted.id(), defaulted.brand(), defaulted.lastFour(),
                defaulted.expiresOn(), defaulted.defaultMethod());
    }
}
