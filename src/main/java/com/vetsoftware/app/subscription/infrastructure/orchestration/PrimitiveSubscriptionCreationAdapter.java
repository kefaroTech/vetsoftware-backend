package com.vetsoftware.app.subscription.infrastructure.orchestration;

import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.CreateSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.out.ResolvedSubscriptionCreationPort;
import org.springframework.stereotype.Component;

/**
 * Delega en la única primitiva que persiste contratos y snapshots resueltos.
 */
@Component
public class PrimitiveSubscriptionCreationAdapter implements ResolvedSubscriptionCreationPort {

    private final CreateSubscriptionUseCase createSubscriptionUseCase;

    public PrimitiveSubscriptionCreationAdapter(
            CreateSubscriptionUseCase createSubscriptionUseCase) {
        this.createSubscriptionUseCase = createSubscriptionUseCase;
    }

    @Override
    public SubscriptionDto create(CreateSubscriptionCommand command) {
        return createSubscriptionUseCase.execute(command);
    }
}
