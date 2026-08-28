package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.OutageAffectedCompanyDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.ListOutageAffectedCompaniesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageCompanyRepository;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El reparto de una caida.
 *
 * <p>
 * Comprueba que la caida existe antes de listar: sin eso, un identificador
 * inventado devolveria una pagina vacia —indistinguible de una caida real que
 * todavia no se ha repartido— y quien la mirara concluiria que no alcanzo a
 * nadie.
 */
@Observed(name = "external.invoicing.outage.company.list")
@Service
public class ListOutageAffectedCompaniesService implements ListOutageAffectedCompaniesUseCase {

    private final ExternalInvoicingOutageCompanyRepository repository;
    private final ExternalInvoicingOutageRepository outageRepository;

    public ListOutageAffectedCompaniesService(ExternalInvoicingOutageCompanyRepository repository,
            ExternalInvoicingOutageRepository outageRepository) {
        this.repository = repository;
        this.outageRepository = outageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OutageAffectedCompanyDto> listByOutage(Long outageId, int page,
            int pageSize) {
        if (outageRepository.findById(outageId).isEmpty())
            throw new ExternalInvoicingOutageNotFoundException(outageId);
        return repository.findAllByOutageId(outageId, page, pageSize)
                .map(OutageAffectedCompanyDto::from);
    }
}
