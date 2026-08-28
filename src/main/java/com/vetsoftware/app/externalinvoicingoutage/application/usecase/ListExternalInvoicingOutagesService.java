package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.ListExternalInvoicingOutagesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El historico de caidas, paginado.
 *
 * <p>
 * Los totales son los de la consulta y no se recalculan sobre el contenido ya
 * paginado: {@code PageResult.map} conserva los cinco campos que trae el
 * adaptador.
 */
@Observed(name = "external.invoicing.outage.list")
@Service
public class ListExternalInvoicingOutagesService implements ListExternalInvoicingOutagesUseCase {

    private final ExternalInvoicingOutageRepository repository;

    public ListExternalInvoicingOutagesService(ExternalInvoicingOutageRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExternalInvoicingOutageDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(ExternalInvoicingOutageDto::from);
    }
}
