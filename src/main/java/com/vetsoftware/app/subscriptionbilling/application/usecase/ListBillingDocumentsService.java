package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListBillingDocumentsUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/** Las cuentas de cobro de una clínica, paginadas y acotadas por su empresa. */
@Observed(name = "subscription.billing.document.list")
@Service
public class ListBillingDocumentsService implements ListBillingDocumentsUseCase {

    private final BillingDocumentRepository repository;

    public ListBillingDocumentsService(BillingDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<BillingDocumentDto> listByCompany(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(BillingDocumentDto::from);
    }
}
