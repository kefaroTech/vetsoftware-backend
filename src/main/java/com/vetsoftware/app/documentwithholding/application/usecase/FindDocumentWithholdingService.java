package com.vetsoftware.app.documentwithholding.application.usecase;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.application.port.in.FindDocumentWithholdingUseCase;
import com.vetsoftware.app.documentwithholding.application.port.out.DocumentWithholdingRepository;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholdingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "document.withholding.find")
@Service
public class FindDocumentWithholdingService implements FindDocumentWithholdingUseCase {

    private final DocumentWithholdingRepository repository;

    public FindDocumentWithholdingService(DocumentWithholdingRepository repository) {
        this.repository = repository;
    }

    /**
     * La retencion de otra empresa sale como <strong>no encontrada</strong> y no
     * como prohibida, y esa es la respuesta correcta: un 403 confirmaria que la
     * fila existe, y con ids consecutivos eso es un censo de las retenciones de la
     * competencia.
     */
    @Override
    public DocumentWithholdingDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(DocumentWithholdingDto::from)
                .orElseThrow(() -> new DocumentWithholdingNotFoundException(id));
    }
}
