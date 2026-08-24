package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.application.dto.SubscriptionQuoteSnapshot;
import java.util.Optional;

/** Lee una quote únicamente dentro de la empresa que firmará el contrato. */
public interface SubscriptionQuoteSnapshotPort {

    Optional<SubscriptionQuoteSnapshot> findByIdAndCompanyId(Long quoteId, Long companyId);
}
