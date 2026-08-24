package com.vetsoftware.app.dunning.application.port.out;

import com.vetsoftware.app.dunning.domain.DunningSubscriptionSnapshot;
import com.vetsoftware.app.dunning.domain.DunningSubscriptionStatus;
import java.util.Optional;

public interface DunningSubscriptionPort {

    Optional<DunningSubscriptionSnapshot> lockByIdAndCompanyId(Long subscriptionId, Long companyId);

    void changeStatus(Long subscriptionId, Long companyId, DunningSubscriptionStatus status,
            String reason, String actor);
}
