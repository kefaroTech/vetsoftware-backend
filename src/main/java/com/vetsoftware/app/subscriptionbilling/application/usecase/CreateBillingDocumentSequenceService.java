package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.CreateBillingDocumentSequenceCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentSequenceDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.CreateBillingDocumentSequenceUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentSequenceRepository;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequence;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequenceAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Declara una serie del consecutivo interno.
 *
 * <p>
 * Comprueba el duplicado antes de insertar para dar un 409 legible en vez de
 * una violación de {@code uq_billing_document_sequences_prefix} convertida en
 * 500. La constraint sigue siendo la última línea: dos altas simultáneas del
 * mismo prefijo pasan las dos la comprobación y una de ellas la encuentra al
 * insertar, que es exactamente lo que tiene que pasar.
 */
@Observed(name = "subscription.billing.sequence.create")
@Service
public class CreateBillingDocumentSequenceService implements CreateBillingDocumentSequenceUseCase {

    private final BillingDocumentSequenceRepository repository;
    private final Clock clock;

    public CreateBillingDocumentSequenceService(BillingDocumentSequenceRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BillingDocumentSequenceDto execute(CreateBillingDocumentSequenceCommand command) {
        repository.findByPrefix(command.prefix()).ifPresent(existing -> {
            throw new BillingDocumentSequenceAlreadyExistsException(command.prefix());
        });
        BillingDocumentSequence sequence = BillingDocumentSequence.create(command.prefix(), clock);
        return BillingDocumentSequenceDto.from(repository.save(sequence));
    }
}
