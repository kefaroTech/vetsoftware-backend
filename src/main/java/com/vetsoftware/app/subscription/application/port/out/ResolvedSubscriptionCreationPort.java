package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;

/** Acceso local a la primitiva que persiste snapshots ya resueltos. */
public interface ResolvedSubscriptionCreationPort {

    SubscriptionDto create(CreateSubscriptionCommand command);
}
