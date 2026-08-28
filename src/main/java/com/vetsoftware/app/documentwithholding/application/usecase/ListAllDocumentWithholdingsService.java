package com.vetsoftware.app.documentwithholding.application.usecase;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.application.port.in.ListAllDocumentWithholdingsUseCase;
import com.vetsoftware.app.documentwithholding.application.port.out.DocumentWithholdingRepository;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * El filtro por empresa es opcional porque lo elige la consola de plataforma,
 * no un tenant: el puerto esta cerrado a {@code hasRole('SYSTEM')} y un
 * principal SYSTEM no tiene empresa propia. Con {@code companyId} acota, sin el
 * barre.
 */
@Observed(name = "document.withholding.list.all")
@Service
public class ListAllDocumentWithholdingsService implements ListAllDocumentWithholdingsUseCase {

    private final DocumentWithholdingRepository repository;

    public ListAllDocumentWithholdingsService(DocumentWithholdingRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<DocumentWithholdingDto> listAll(Long companyId, int page, int pageSize) {
        PageResult<DocumentWithholding> withholdings = companyId == null
                ? repository.findAll(page, pageSize)
                : repository.findAllByCompanyId(companyId, page, pageSize);
        return withholdings.map(DocumentWithholdingDto::from);
    }
}
