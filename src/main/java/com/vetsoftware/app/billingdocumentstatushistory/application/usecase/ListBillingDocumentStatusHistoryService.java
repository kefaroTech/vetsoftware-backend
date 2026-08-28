package com.vetsoftware.app.billingdocumentstatushistory.application.usecase;

import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.ListBillingDocumentStatusHistoryUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.out.BillingDocumentStatusHistoryRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "billing.document.status.history.list.by.document")
@Service
public class ListBillingDocumentStatusHistoryService
        implements
            ListBillingDocumentStatusHistoryUseCase {

    private final BillingDocumentStatusHistoryRepository repository;

    public ListBillingDocumentStatusHistoryService(
            BillingDocumentStatusHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<BillingDocumentStatusHistoryDto> listByDocument(Long companyId,
            Long billingDocumentId, int page, int pageSize) {
        return repository.findAllByCompanyIdAndBillingDocumentId(companyId, billingDocumentId, page,
                pageSize).map(BillingDocumentStatusHistoryDto::from);
    }
}
