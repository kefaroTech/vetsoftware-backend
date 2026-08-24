package com.vetsoftware.app.dunning.application.usecase;

import com.vetsoftware.app.dunning.application.command.RecordDunningEventCommand;
import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.dunning.application.port.in.RecordDunningEventUseCase;
import com.vetsoftware.app.dunning.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.dunning.application.port.out.DunningEventRepository;
import com.vetsoftware.app.dunning.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.dunning.domain.BillingDocumentRef;
import com.vetsoftware.app.dunning.domain.DunningEvent;
import com.vetsoftware.app.dunning.domain.SubscriptionRef;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anota un hito del expediente de cobranza.
 *
 * <p>
 * Las dos referencias se resuelven <strong>acotadas por empresa</strong>: es lo
 * que impide que un evento propio cuelgue del contrato o de la factura de otra
 * clinica. El resto de las invariantes -canal obligatorio en un recordatorio,
 * dias de mora no negativos- viven en el constructor de la entidad, no aqui.
 *
 * <p>
 * <strong>Este servicio no calcula nada.</strong> La aritmetica de la mora no
 * esta especificada en el modelo y no se inventa aqui: quien decide que un
 * contrato entra en gracia o baja a solo lectura es el motor de cobranza, que
 * todavia no existe. Este slice deja constancia de lo que le llega.
 */
@Observed(name = "dunning.event.record")
@Service
public class RecordDunningEventService implements RecordDunningEventUseCase {

    private final DunningEventRepository repository;
    private final SubscriptionQueryPort subscriptionQueryPort;
    private final BillingDocumentQueryPort billingDocumentQueryPort;
    private final Clock clock;

    public RecordDunningEventService(DunningEventRepository repository,
            SubscriptionQueryPort subscriptionQueryPort,
            BillingDocumentQueryPort billingDocumentQueryPort, Clock clock) {
        this.repository = repository;
        this.subscriptionQueryPort = subscriptionQueryPort;
        this.billingDocumentQueryPort = billingDocumentQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DunningEventDto execute(RecordDunningEventCommand command) {
        SubscriptionRef subscription = subscriptionQueryPort
                .findByIdAndCompanyId(command.subscriptionId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));
        BillingDocumentRef billingDocument = resolveBillingDocument(command);
        LocalDateTime now = LocalDateTime.now(clock);
        DunningEvent event = DunningEvent.record(command.companyId(), subscription, billingDocument,
                command.eventType(), command.daysOverdue(), command.channel(), command.detail(),
                command.occurredAt() == null ? now : command.occurredAt(), now);
        return DunningEventDto.from(repository.save(event));
    }

    /**
     * Nulo en los eventos de contrato ({@code READ_ONLY_APPLIED},
     * {@code REACTIVATED}), que no cuelgan de ninguna factura concreta.
     */
    private BillingDocumentRef resolveBillingDocument(RecordDunningEventCommand command) {
        if (command.billingDocumentId() == null)
            return null;
        return billingDocumentQueryPort
                .findByIdAndCompanyId(command.billingDocumentId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "BillingDocument not found: " + command.billingDocumentId()));
    }
}
