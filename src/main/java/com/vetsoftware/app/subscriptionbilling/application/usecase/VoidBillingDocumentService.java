package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.VoidBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.VoidBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocumentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anula un documento que todavía no existe fuera.
 *
 * <p>
 * Uno con factura externa ya registrada <b>no</b> se anula aquí: se corrige con
 * una nota crédito encadenada. Lo rechaza el propio agregado, no este servicio,
 * para que la regla siga valiendo desde cualquier otro caller.
 */
@Observed(name = "subscription.billing.document.void")
@Service
public class VoidBillingDocumentService implements VoidBillingDocumentUseCase {

    private final BillingDocumentRepository repository;

    public VoidBillingDocumentService(BillingDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public BillingDocumentDto execute(VoidBillingDocumentCommand command) {
        SubscriptionBillingDocument document = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionBillingDocumentNotFoundException(command.id()));
        document.voidDocument();
        return BillingDocumentDto.from(repository.save(document));
    }
}
