package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.ListExternalInvoiceReconciliationsUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * El filtro por empresa es opcional porque lo elige la consola de plataforma,
 * no un tenant: el puerto esta cerrado a {@code hasRole('SYSTEM')} y un
 * principal SYSTEM no tiene empresa propia. Con {@code companyId} acota, sin el
 * barre.
 */
@Observed(name = "external.invoice.reconciliation.list")
@Service
public class ListExternalInvoiceReconciliationsService
        implements
            ListExternalInvoiceReconciliationsUseCase {

    private final ExternalInvoiceReconciliationRepository repository;

    public ListExternalInvoiceReconciliationsService(
            ExternalInvoiceReconciliationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<ExternalInvoiceReconciliationDto> listAll(Long companyId, int page,
            int pageSize) {
        PageResult<ExternalInvoiceReconciliation> reconciliations = companyId == null
                ? repository.findAll(page, pageSize)
                : repository.findAllByCompanyId(companyId, page, pageSize);
        return reconciliations.map(ExternalInvoiceReconciliationDto::from);
    }
}
