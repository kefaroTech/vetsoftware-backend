package com.vetsoftware.app.subscription.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionProrationChargePort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionProrationLine;
import com.vetsoftware.app.subscriptionbilling.application.command.CreateSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.port.in.CreateSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import org.springframework.stereotype.Component;

/**
 * Cruza a {@code subscriptionbilling} <b>por su puerto de entrada</b>, no por
 * su repositorio. Mismo patron que {@code SubscriptionPeriodAdvanceAdapter}.
 *
 * <p>
 * <b>Va envuelto en {@link SystemAuthRunner} por lo mismo que
 * {@code EntitlementRecalculationAdapter}</b>: el otrosi que dispara este cargo
 * corre bajo el principal del empleado del tenant que amplio su contrato, y
 * {@code CreateSubscriptionChargeUseCase} exige {@code hasRole('SYSTEM')} a
 * secas. Sin la escalada, ampliar un modulo a mitad de ciclo revertiria con 403
 * y sin rastro del cambio.
 *
 * <p>
 * <b>No es I/O externo dentro de una transaccion</b>: es una escritura mas en
 * la misma base, dentro de la transaccion del otrosi que la llama — la regla de
 * efectos externos no aplica aqui, igual que no aplica al recalculo de
 * permisos.
 */
@Component
public class SubscriptionBillingProrationChargeAdapter implements SubscriptionProrationChargePort {

    private final CreateSubscriptionChargeUseCase createChargeUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public SubscriptionBillingProrationChargeAdapter(
            CreateSubscriptionChargeUseCase createChargeUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.createChargeUseCase = createChargeUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void chargeProration(SubscriptionProrationLine line) {
        systemAuthRunner.run(() -> createChargeUseCase.execute(new CreateSubscriptionChargeCommand(
                line.companyId(), line.subscriptionId(), line.subscriptionItemId(),
                ChargeType.PRORATION, line.description(), line.servicePeriodStart(),
                line.servicePeriodEnd(), line.quantity(), line.unitAmount(), line.subtotalAmount(),
                line.taxRate(), traducir(line.taxTreatment()), line.prorationDays(),
                line.periodDays(), line.amendmentId())));
    }

    private static com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment traducir(
            com.vetsoftware.app.subscription.domain.TaxTreatment taxTreatment) {
        return switch (taxTreatment) {
            case TAXED -> com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment.TAXED;
            case EXEMPT -> com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment.EXEMPT;
            case EXCLUDED -> com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment.EXCLUDED;
        };
    }
}
