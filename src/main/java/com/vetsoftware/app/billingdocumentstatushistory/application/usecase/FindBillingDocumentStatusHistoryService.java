package com.vetsoftware.app.billingdocumentstatushistory.application.usecase;

import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.FindBillingDocumentStatusHistoryUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.out.BillingDocumentStatusHistoryRepository;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistoryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "billing.document.status.history.find")
@Service
public class FindBillingDocumentStatusHistoryService
        implements
            FindBillingDocumentStatusHistoryUseCase {

    private final BillingDocumentStatusHistoryRepository repository;

    public FindBillingDocumentStatusHistoryService(
            BillingDocumentStatusHistoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Carga acotada por empresa, que es la unica que el puerto de salida ofrece
     * ({@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}). El fotograma de otra empresa sale
     * como no encontrado y no como prohibido.
     */
    @Override
    public BillingDocumentStatusHistoryDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId)
                .map(BillingDocumentStatusHistoryDto::from)
                .orElseThrow(() -> new BillingDocumentStatusHistoryNotFoundException(id));
    }
}
