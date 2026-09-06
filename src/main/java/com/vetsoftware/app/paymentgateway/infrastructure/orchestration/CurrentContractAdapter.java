package com.vetsoftware.app.paymentgateway.infrastructure.orchestration;

import com.vetsoftware.app.paymentgateway.application.port.out.CurrentContractQueryPort;
import com.vetsoftware.app.paymentgateway.domain.CurrentContractRef;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.FindCurrentSubscriptionUseCase;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Delega en {@code subscription} para saber cuál es el contrato vigente.
 *
 * <p>
 * <strong>Sin {@code SystemAuthRunner}</strong>:
 * {@code FindCurrentSubscriptionUseCase} exige {@code subscription.read} y
 * {@code isMyCompany(companyId)}, que es exactamente lo que ya exige
 * {@code FindFirstPeriodPaymentUseCase} —el único llamador de este puerto—. No
 * hay escalada que hacer.
 */
@Component
public class CurrentContractAdapter implements CurrentContractQueryPort {

    private final FindCurrentSubscriptionUseCase findCurrentSubscriptionUseCase;

    public CurrentContractAdapter(FindCurrentSubscriptionUseCase findCurrentSubscriptionUseCase) {
        this.findCurrentSubscriptionUseCase = findCurrentSubscriptionUseCase;
    }

    @Override
    public Optional<CurrentContractRef> findCurrent(Long companyId) {
        try {
            SubscriptionDto contract = findCurrentSubscriptionUseCase.findCurrent(companyId);
            return Optional.of(new CurrentContractRef(contract.id(), contract.subscriptionNumber(),
                    contract.currentPeriodStart(), contract.currentPeriodEnd()));
        } catch (SubscriptionNotFoundException e) {
            return Optional.empty();
        }
    }
}
