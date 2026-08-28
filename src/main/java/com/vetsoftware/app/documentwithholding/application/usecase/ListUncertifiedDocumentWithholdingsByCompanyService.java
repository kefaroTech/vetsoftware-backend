package com.vetsoftware.app.documentwithholding.application.usecase;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.application.port.in.ListUncertifiedDocumentWithholdingsByCompanyUseCase;
import com.vetsoftware.app.documentwithholding.application.port.out.DocumentWithholdingRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * La vigilancia acotada a una empresa: la lista con la que el cliente reclama
 * los certificados que le faltan de un ano.
 */
@Observed(name = "document.withholding.list.uncertified.by.company")
@Service
public class ListUncertifiedDocumentWithholdingsByCompanyService
        implements
            ListUncertifiedDocumentWithholdingsByCompanyUseCase {

    private final DocumentWithholdingRepository repository;

    public ListUncertifiedDocumentWithholdingsByCompanyService(
            DocumentWithholdingRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<DocumentWithholdingDto> listUncertifiedByCompany(Long companyId,
            int fiscalYear, int page, int pageSize) {
        return repository
                .findAllUncertifiedByCompanyIdAndFiscalYear(companyId, fiscalYear, page, pageSize)
                .map(DocumentWithholdingDto::from);
    }
}
