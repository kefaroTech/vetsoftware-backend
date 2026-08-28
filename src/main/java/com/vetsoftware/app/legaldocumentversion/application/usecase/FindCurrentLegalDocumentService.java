package com.vetsoftware.app.legaldocumentversion.application.usecase;

import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import com.vetsoftware.app.legaldocumentversion.application.port.in.FindCurrentLegalDocumentUseCase;
import com.vetsoftware.app.legaldocumentversion.application.port.out.LegalDocumentVersionRepository;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "legaldocument.current")
@Service
public class FindCurrentLegalDocumentService implements FindCurrentLegalDocumentUseCase {

    private final LegalDocumentVersionRepository repository;

    public FindCurrentLegalDocumentService(LegalDocumentVersionRepository repository) {
        this.repository = repository;
    }

    @Override
    public LegalDocumentVersionDto findCurrentByCode(String code, Long companyId) {
        return repository.findCurrentByCode(code).map(LegalDocumentVersionDto::from)
                .orElseThrow(() -> new LegalDocumentVersionNotFoundException(code));
    }
}
