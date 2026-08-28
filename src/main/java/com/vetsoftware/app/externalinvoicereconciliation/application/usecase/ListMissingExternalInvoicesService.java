package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.ListMissingExternalInvoicesUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * La bandeja de lo que nadie facturo.
 *
 * <p>
 * El estado va fijado <strong>aqui</strong> y no llega por parametro: si el
 * llamante pudiera elegirlo, este caso de uso seria otro barrido general mas y
 * la consulta que importa volveria a depender de que alguien se acuerde de
 * filtrar por {@code MISSING_EXTERNAL}. Fijarlo es lo que convierte la bandeja
 * en un sitio al que se entra, no en una opcion de un desplegable.
 */
@Observed(name = "external.invoice.reconciliation.list.missing")
@Service
public class ListMissingExternalInvoicesService implements ListMissingExternalInvoicesUseCase {

    private final ExternalInvoiceReconciliationRepository repository;

    public ListMissingExternalInvoicesService(ExternalInvoiceReconciliationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<ExternalInvoiceReconciliationDto> listMissing(int page, int pageSize) {
        return repository.findAllByStatus(ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL,
                page, pageSize).map(ExternalInvoiceReconciliationDto::from);
    }
}
