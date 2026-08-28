package com.vetsoftware.app.legaldocumentversion.application.usecase;

import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import com.vetsoftware.app.legaldocumentversion.application.port.in.ListLegalDocumentVersionsUseCase;
import com.vetsoftware.app.legaldocumentversion.application.port.out.LegalDocumentVersionRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "legaldocument.list")
@Service
public class ListLegalDocumentVersionsService implements ListLegalDocumentVersionsUseCase {

    private final LegalDocumentVersionRepository repository;

    public ListLegalDocumentVersionsService(LegalDocumentVersionRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<LegalDocumentVersionDto> listByCode(String code, Long companyId, int page,
            int pageSize) {
        return repository.findAllByCode(code, page, pageSize).map(LegalDocumentVersionDto::from);
    }
}
