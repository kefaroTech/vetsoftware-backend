package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.FindBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocumentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/** Consulta una cuenta de cobro propia, con su desglose fiscal. */
@Observed(name = "subscription.billing.document.find")
@Service
public class FindBillingDocumentService implements FindBillingDocumentUseCase {

    private final BillingDocumentRepository repository;

    public FindBillingDocumentService(BillingDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public BillingDocumentDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(BillingDocumentDto::from)
                .orElseThrow(() -> new SubscriptionBillingDocumentNotFoundException(id));
    }
}
