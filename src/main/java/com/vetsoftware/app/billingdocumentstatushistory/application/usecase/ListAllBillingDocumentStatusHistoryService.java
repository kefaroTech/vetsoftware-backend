package com.vetsoftware.app.billingdocumentstatushistory.application.usecase;

import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.ListAllBillingDocumentStatusHistoryUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.out.BillingDocumentStatusHistoryRepository;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistory;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * El filtro por empresa es opcional porque lo elige la consola de plataforma,
 * no un tenant: el puerto esta cerrado a {@code hasRole('SYSTEM')} y un
 * principal SYSTEM no tiene empresa propia. Con {@code companyId} acota, sin el
 * barre.
 */
@Observed(name = "billing.document.status.history.list.all")
@Service
public class ListAllBillingDocumentStatusHistoryService
        implements
            ListAllBillingDocumentStatusHistoryUseCase {

    private final BillingDocumentStatusHistoryRepository repository;

    public ListAllBillingDocumentStatusHistoryService(
            BillingDocumentStatusHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<BillingDocumentStatusHistoryDto> listAll(Long companyId, int page,
            int pageSize) {
        PageResult<BillingDocumentStatusHistory> entries = companyId == null
                ? repository.findAll(page, pageSize)
                : repository.findAllByCompanyId(companyId, page, pageSize);
        return entries.map(BillingDocumentStatusHistoryDto::from);
    }
}
