package com.vetsoftware.app.subscriptionitemlimit.application.usecase;

import com.vetsoftware.app.subscriptionitemlimit.application.command.FreezeSubscriptionItemLimitCommand;
import com.vetsoftware.app.subscriptionitemlimit.application.dto.SubscriptionItemLimitDto;
import com.vetsoftware.app.subscriptionitemlimit.application.port.in.FreezeSubscriptionItemLimitUseCase;
import com.vetsoftware.app.subscriptionitemlimit.application.port.out.SubscriptionItemLimitRepository;
import com.vetsoftware.app.subscriptionitemlimit.domain.SubscriptionItemLimit;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Congela el techo de fábrica en la línea del contrato. */
@Service
public class FreezeSubscriptionItemLimitService implements FreezeSubscriptionItemLimitUseCase {

    private final SubscriptionItemLimitRepository repository;
    private final Clock clock;

    public FreezeSubscriptionItemLimitService(SubscriptionItemLimitRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionItemLimitDto execute(FreezeSubscriptionItemLimitCommand command) {
        SubscriptionItemLimit limit = SubscriptionItemLimit.freeze(command.companyId(),
                command.subscriptionItemId(), command.limitDimensionId(), command.measureKind(),
                command.mode(), command.limitQuantity(), command.resetPeriod(),
                command.enforcement(), command.overageUnitAmount(), command.warnThreshold(),
                LocalDateTime.now(clock));
        return SubscriptionItemLimitDto.from(repository.save(limit));
    }
}
