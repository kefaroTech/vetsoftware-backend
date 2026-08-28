package com.vetsoftware.app.billingdocumentstatushistory.application.usecase;

import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.ListBillingDocumentStatusChangesByStatusUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.out.BillingDocumentStatusHistoryRepository;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "billing.document.status.history.list.by.status")
@Service
public class ListBillingDocumentStatusChangesByStatusService
        implements
            ListBillingDocumentStatusChangesByStatusUseCase {

    private final BillingDocumentStatusHistoryRepository repository;

    public ListBillingDocumentStatusChangesByStatusService(
            BillingDocumentStatusHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<BillingDocumentStatusHistoryDto> listByCompanyAndToStatus(Long companyId,
            BillingDocumentStatus toStatus, int page, int pageSize) {
        return repository.findAllByCompanyIdAndToStatus(companyId, toStatus, page, pageSize)
                .map(BillingDocumentStatusHistoryDto::from);
    }
}
