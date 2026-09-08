package com.vetsoftware.app.quote.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.quote.application.port.out.PaymentSourceDefaultingPort;
import com.vetsoftware.app.subscriptionpaymentmethod.application.command.SetDefaultPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.SetDefaultPaymentMethodUseCase;
import org.springframework.stereotype.Component;

/**
 * Marca el medio de pago como predeterminado, escalando a {@code SYSTEM}: el
 * rol ADMIN ya se comprobó en {@code PurchaseModulesService}, y no se le exige
 * además el permiso granular {@code subscriptionPaymentMethod.update} para
 * completar una compra que ya autorizó.
 */
@Component
public class PaymentSourceDefaultingAdapter implements PaymentSourceDefaultingPort {

    private final SetDefaultPaymentMethodUseCase setDefaultPaymentMethodUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public PaymentSourceDefaultingAdapter(
            SetDefaultPaymentMethodUseCase setDefaultPaymentMethodUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.setDefaultPaymentMethodUseCase = setDefaultPaymentMethodUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void markDefaultIfNeeded(Long companyId, Long paymentSourceId) {
        systemAuthRunner.run(() -> setDefaultPaymentMethodUseCase
                .execute(new SetDefaultPaymentMethodCommand(paymentSourceId, companyId)));
    }
}
