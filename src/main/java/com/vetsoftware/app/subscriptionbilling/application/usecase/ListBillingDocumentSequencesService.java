package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentSequenceDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListBillingDocumentSequencesUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentSequenceRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Las series del consecutivo interno.
 *
 * <p>
 * {@code billing_document_sequences} no tiene {@code company_id} y su
 * repositorio no declara ningún método que filtre por empresa, así que no hay
 * nada por lo que acotar: es un contador de plataforma y su puerto está cerrado
 * a {@code hasRole("SYSTEM")} a secas.
 */
@Observed(name = "subscription.billing.sequence.list")
@Service
public class ListBillingDocumentSequencesService implements ListBillingDocumentSequencesUseCase {

    private final BillingDocumentSequenceRepository repository;

    public ListBillingDocumentSequencesService(BillingDocumentSequenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<BillingDocumentSequenceDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(BillingDocumentSequenceDto::from);
    }
}
