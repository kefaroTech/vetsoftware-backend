package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.RegisterExternalInvoiceCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.RegisterExternalInvoiceUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingPolicyPort;
import com.vetsoftware.app.subscriptionbilling.domain.ExternalInvoiceReference;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocumentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Captura aquí la referencia de la factura emitida fuera y, con ella, el
 * vencimiento.
 *
 * <p>
 * <b>El vencimiento se cuenta desde {@code externalIssuedAt}, la fecha
 * fiscal.</b> El {@link Clock} de este servicio se usa <b>solo</b> para el
 * sello de auditoría {@code external_registered_at} —cuándo se capturó aquí— y
 * nunca para el vencimiento: contarlo desde el momento del registro, o desde el
 * cálculo interno del documento, suspendería cuentas por un retraso
 * administrativo propio y no del cliente. Es la diferencia entre «el cliente no
 * pagó» y «nosotros tardamos en facturar».
 */
@Observed(name = "subscription.billing.document.register.external")
@Service
public class RegisterExternalInvoiceService implements RegisterExternalInvoiceUseCase {

    private final BillingDocumentRepository repository;
    private final BillingPolicyPort billingPolicyPort;
    private final Clock clock;

    public RegisterExternalInvoiceService(BillingDocumentRepository repository,
            BillingPolicyPort billingPolicyPort, Clock clock) {
        this.repository = repository;
        this.billingPolicyPort = billingPolicyPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BillingDocumentDto execute(RegisterExternalInvoiceCommand command) {
        SubscriptionBillingDocument document = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionBillingDocumentNotFoundException(command.id()));
        ExternalInvoiceReference reference = new ExternalInvoiceReference(command.invoiceNumber(),
                command.cufe(), command.issuedAt(), command.provider(), LocalDateTime.now(clock),
                command.registeredBySystemUserId());
        document.registerExternalInvoice(reference, billingPolicyPort.defaultPaymentTermDays());
        return BillingDocumentDto.from(repository.save(document));
    }
}
