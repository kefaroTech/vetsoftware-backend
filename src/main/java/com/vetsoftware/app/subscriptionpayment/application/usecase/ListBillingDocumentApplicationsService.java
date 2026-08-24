package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListBillingDocumentApplicationsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentApplicationRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.payment.application.list.by.document")
@Service
public class ListBillingDocumentApplicationsService
        implements
            ListBillingDocumentApplicationsUseCase {

    private final BillingDocumentApplicationRepository repository;

    public ListBillingDocumentApplicationsService(BillingDocumentApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<BillingDocumentApplicationDto> listByTargetDocument(Long targetDocumentId,
            Long companyId, int page, int pageSize) {
        return repository
                .findAllByTargetDocumentIdAndCompanyId(targetDocumentId, companyId, page, pageSize)
                .map(BillingDocumentApplicationDto::from);
    }
}
