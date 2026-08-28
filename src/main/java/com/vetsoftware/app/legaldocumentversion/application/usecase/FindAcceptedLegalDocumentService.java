package com.vetsoftware.app.legaldocumentversion.application.usecase;

import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import com.vetsoftware.app.legaldocumentversion.application.port.in.FindAcceptedLegalDocumentUseCase;
import com.vetsoftware.app.legaldocumentversion.application.port.out.LegalDocumentVersionRepository;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Devuelve el texto que corresponde a una huella, <strong>aunque ya haya sido
 * sucedido</strong>. Caer en la version vigente cuando la huella no casa seria
 * el peor de los comportamientos posibles: le ensenaria al cliente un texto que
 * nunca acepto y lo llamaria prueba.
 */
@Observed(name = "legaldocument.accepted")
@Service
public class FindAcceptedLegalDocumentService implements FindAcceptedLegalDocumentUseCase {

    private final LegalDocumentVersionRepository repository;

    public FindAcceptedLegalDocumentService(LegalDocumentVersionRepository repository) {
        this.repository = repository;
    }

    @Override
    public LegalDocumentVersionDto findByCodeAndHash(String code, String contentHash,
            Long companyId) {
        return repository.findByCodeAndContentHash(code, contentHash)
                .map(LegalDocumentVersionDto::from)
                .orElseThrow(() -> new LegalDocumentVersionNotFoundException(code, contentHash));
    }
}
