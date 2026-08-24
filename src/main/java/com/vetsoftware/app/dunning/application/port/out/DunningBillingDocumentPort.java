package com.vetsoftware.app.dunning.application.port.out;

import com.vetsoftware.app.dunning.domain.DunningBillingDocumentSnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DunningBillingDocumentPort {

    Optional<DunningBillingDocumentSnapshot> lockByIdAndCompanyId(Long documentId, Long companyId);

    Optional<DunningBillingDocumentSnapshot> findOldestOverdue(Long subscriptionId, Long companyId,
            LocalDate today);

    List<DunningBillingDocumentSnapshot> lockOverdueBatchAfter(LocalDate today, long afterId,
            int batchSize);
}
