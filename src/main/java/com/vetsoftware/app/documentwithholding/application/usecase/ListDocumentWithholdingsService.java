package com.vetsoftware.app.documentwithholding.application.usecase;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.application.port.in.ListDocumentWithholdingsUseCase;
import com.vetsoftware.app.documentwithholding.application.port.out.DocumentWithholdingRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "document.withholding.list.by.company")
@Service
public class ListDocumentWithholdingsService implements ListDocumentWithholdingsUseCase {

    private final DocumentWithholdingRepository repository;

    public ListDocumentWithholdingsService(DocumentWithholdingRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<DocumentWithholdingDto> listByCompany(Long companyId, int page,
            int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(DocumentWithholdingDto::from);
    }
}
