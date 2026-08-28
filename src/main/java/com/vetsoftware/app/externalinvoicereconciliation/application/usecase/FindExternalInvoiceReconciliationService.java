package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.FindExternalInvoiceReconciliationUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * La carga es ancha -{@code findById(id)} sin empresa- y esa es la forma
 * correcta aqui: el puerto esta cerrado a {@code hasRole('SYSTEM')} a secas y
 * un principal SYSTEM no tiene empresa propia. Es la exencion que
 * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) declara expresamente.
 */
@Observed(name = "external.invoice.reconciliation.find")
@Service
public class FindExternalInvoiceReconciliationService
        implements
            FindExternalInvoiceReconciliationUseCase {

    private final ExternalInvoiceReconciliationRepository repository;

    public FindExternalInvoiceReconciliationService(
            ExternalInvoiceReconciliationRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExternalInvoiceReconciliationDto findById(Long id) {
        return repository.findById(id).map(ExternalInvoiceReconciliationDto::from)
                .orElseThrow(() -> new ExternalInvoiceReconciliationNotFoundException(id));
    }
}
