package com.vetsoftware.app.subscription.infrastructure.payment;

import com.vetsoftware.app.paymentgateway.application.command.ChargeContractFirstPeriodCommand;
import com.vetsoftware.app.paymentgateway.application.dto.FirstPeriodChargeDto;
import com.vetsoftware.app.paymentgateway.application.port.in.ChargeContractFirstPeriodUseCase;
import com.vetsoftware.app.subscription.application.dto.ContractPaymentOutcome;
import com.vetsoftware.app.subscription.application.port.out.ContractPaymentPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Conecta el cobro real: delega en {@code ChargeContractFirstPeriodUseCase}
 * (rodaja {@code paymentgateway}) y traduce su {@link FirstPeriodChargeDto} a
 * {@link ContractPaymentOutcome}.
 *
 * <p>
 * <strong>Sin {@code SystemAuthRunner}.</strong>
 * {@code SettleNewContractService} —el único llamador de este adaptador— ya
 * corre bajo {@code SYSTEM} desde {@code AcceptedQuoteSubscriptionProvisioner},
 * y {@code ChargeContractFirstPeriodUseCase} exige exactamente eso.
 */
@Component
public class WompiContractPaymentAdapter implements ContractPaymentPort {

    private final ChargeContractFirstPeriodUseCase chargeUseCase;

    public WompiContractPaymentAdapter(ChargeContractFirstPeriodUseCase chargeUseCase) {
        this.chargeUseCase = chargeUseCase;
    }

    @Override
    public ContractPaymentOutcome chargeFirstPeriod(Long companyId, Long subscriptionId,
            String subscriptionNumber, BillingCycle billingCycle, LocalDate periodStart,
            LocalDate periodEnd) {
        FirstPeriodChargeDto result = chargeUseCase.execute(new ChargeContractFirstPeriodCommand(
                companyId, subscriptionId, subscriptionNumber, periodStart, periodEnd));
        return switch (result.outcome()) {
            case APPROVED -> ContractPaymentOutcome.approved(result.gatewayReference());
            case PENDING -> ContractPaymentOutcome.pending(result.gatewayReference());
            case DECLINED, NOT_CONFIGURED, NO_PAYMENT_METHOD ->
                ContractPaymentOutcome.declined(result.declineReason() != null
                        ? result.declineReason()
                        : result.outcome().name());
        };
    }
}
