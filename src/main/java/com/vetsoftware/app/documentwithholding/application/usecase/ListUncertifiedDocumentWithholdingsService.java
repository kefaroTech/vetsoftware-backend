package com.vetsoftware.app.documentwithholding.application.usecase;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.application.port.in.ListUncertifiedDocumentWithholdingsUseCase;
import com.vetsoftware.app.documentwithholding.application.port.out.DocumentWithholdingRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * El barrido de vigilancia de tesoreria: lo retenido en un ano que aun no tiene
 * certificado, en todas las clinicas.
 *
 * <p>
 * <strong>No acepta {@code companyId} ni siquiera opcional</strong>, a
 * diferencia de {@link ListAllDocumentWithholdingsService}. La razon es que
 * este servicio y {@link ListUncertifiedDocumentWithholdingsByCompanyService}
 * son dos casos de uso distintos con dos autorizaciones distintas: uno lo pide
 * tesoreria para saber cuanto falta por reclamar en toda la plataforma, el otro
 * lo pide el cliente para reclamar lo suyo. Fundirlos en un puerto con filtro
 * opcional obligaria a un SpEL con {@code or hasAuthority}, y omitir el
 * parametro bastaria para leer las retenciones de todos los tenants.
 */
@Observed(name = "document.withholding.list.uncertified")
@Service
public class ListUncertifiedDocumentWithholdingsService
        implements
            ListUncertifiedDocumentWithholdingsUseCase {

    private final DocumentWithholdingRepository repository;

    public ListUncertifiedDocumentWithholdingsService(DocumentWithholdingRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<DocumentWithholdingDto> listUncertified(int fiscalYear, int page,
            int pageSize) {
        return repository.findAllUncertifiedByFiscalYear(fiscalYear, page, pageSize)
                .map(DocumentWithholdingDto::from);
    }
}
