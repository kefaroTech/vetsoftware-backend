package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSubscriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionDto execute(CreateSubscriptionCommand command);
}
