package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.GenerateBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.GenerateBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentSequenceRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentNumber;
import com.vetsoftware.app.subscriptionbilling.domain.DuplicateBillingCycleException;
import com.vetsoftware.app.subscriptionbilling.domain.EmptyBillingDocumentException;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionRef;
import com.vetsoftware.app.subscriptionbilling.domain.TaxBreakdown;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula y numera la cuenta de cobro de un contrato para un <b>periodo
 * exacto</b>.
 *
 * <p>
 * <b>Todo el método es una sola transacción, y ese es el punto.</b> Dentro
 * pasan cuatro cosas que no pueden quedar a medias: se consume el consecutivo
 * con la fila bloqueada, se calcula el desglose fiscal, se guarda el documento
 * y se sellan los cargos. Si algo falla después de consumir el número, el
 * {@code rollback} lo devuelve y la serie <b>no deja huecos</b>. Es la
 * diferencia deliberada con el consecutivo fiscal de la DIAN, que sí se reserva
 * aparte porque allí el hueco es lo prohibido.
 *
 * <p>
 * <b>La barandilla va antes que el número.</b> Comprobar el periodo duplicado
 * después de consumir el consecutivo funcionaría igual —el rollback lo
 * devolvería— pero deja el bloqueo de la fila de la serie tomado durante toda
 * la comprobación, y esa fila la comparten todas las clínicas.
 */
@Observed(name = "subscription.billing.document.generate")
@Service
public class GenerateBillingDocumentService implements GenerateBillingDocumentUseCase {

    private static final DocumentKind KIND = DocumentKind.INVOICE;

    private final BillingDocumentRepository documentRepository;
    private final SubscriptionChargeRepository chargeRepository;
    private final BillingDocumentSequenceRepository sequenceRepository;
    private final SubscriptionQueryPort subscriptionQueryPort;
    private final SubscriptionBillingMetrics metrics;
    private final SubscriptionBillingAuditPort audit;
    private final Clock clock;

    public GenerateBillingDocumentService(BillingDocumentRepository documentRepository,
            SubscriptionChargeRepository chargeRepository,
            BillingDocumentSequenceRepository sequenceRepository,
            SubscriptionQueryPort subscriptionQueryPort, SubscriptionBillingMetrics metrics,
            SubscriptionBillingAuditPort audit, Clock clock) {
        this.documentRepository = documentRepository;
        this.chargeRepository = chargeRepository;
        this.sequenceRepository = sequenceRepository;
        this.subscriptionQueryPort = subscriptionQueryPort;
        this.metrics = metrics;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BillingDocumentDto execute(GenerateBillingDocumentCommand command) {
        SubscriptionRef subscription = subscriptionQueryPort
                .findByIdAndCompanyId(command.subscriptionId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));
        subscription.exigirEmpresa(command.companyId());
        ServicePeriod period = new ServicePeriod(command.periodStart(), command.periodEnd());
        BillingReason reason = command.billingReason();

        // La barandilla contra la doble facturacion: por PERIODO EXACTO, no por mes.
        // Solo mira las facturas de ciclo -es lo que hace la columna [ANADIDA]
        // billing_reason-, de modo que la anual emitida a mitad de agosto y la mensual
        // del dia 1 conviven, y el cambio a plan anual deja de ser irregistrable.
        if (reason == BillingReason.RECURRING_CYCLE && documentRepository.existsRecurringCycle(
                command.companyId(), subscription.id(), period.start(), period.end())) {
            // recordNow y no recordAfterCommit: esto revierte la transaccion, asi que
            // diferirlo al commit descartaria el contador y la barandilla antiduplicados
            // pareceria no haberse disparado nunca.
            metrics.documentRejected(SubscriptionBillingMetrics.Rejection.DUPLICATE_CYCLE);
            throw new DuplicateBillingCycleException(subscription.id(), period.start(),
                    period.end());
        }

        List<SubscriptionCharge> charges = chargeRepository.findPendingByCompanyIdAndSubscription(
                command.companyId(), subscription.id(), period.start(), period.end());
        if (charges.isEmpty()) {
            // Un periodo sin ni un cargo pendiente no es un caso raro: significa que el
            // devengo no corrio. Sin este contador el cierre de mes se salta una empresa
            // en silencio y se descubre cuando el cliente pregunta por su factura.
            metrics.documentRejected(SubscriptionBillingMetrics.Rejection.NO_CHARGES);
            throw new EmptyBillingDocumentException(subscription.id());
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        TaxBreakdown breakdown = TaxBreakdown.of(charges, KIND, command.companyId(), ahora);
        DocumentNumber number = sequenceRepository.nextNumber(KIND.sequencePrefix());
        SubscriptionBillingDocument saved = documentRepository
                .save(SubscriptionBillingDocument.issue(number, command.companyId(),
                        subscription.id(), KIND, reason, period, breakdown, null, clock));

        sellarCargos(charges, command.companyId(), saved.getId());

        metrics.documentIssued(saved.getIssueStatus());
        audit.documentIssued(saved.getId(), saved.getDocumentNumber(), saved.getSubscriptionId(),
                saved.getIssueStatus(), saved.getTotalAmount(), charges.size());
        return BillingDocumentDto.from(saved);
    }

    /**
     * Sella los cargos dentro del documento y comprueba que se sellaron
     * <b>todos</b>.
     *
     * <p>
     * El {@code UPDATE} filtra por {@code status = 'PENDING'}, así que un cargo que
     * dejó de estarlo entre la lectura y el sellado no se actualiza y la cuenta no
     * cuadra. Fallar aquí revierte el documento entero; dejarlo pasar produciría un
     * documento cuyo subtotal no coincide con la suma de sus cargos, que es
     * exactamente el descuadre que la conciliación R6 caza un mes más tarde.
     */
    private void sellarCargos(List<SubscriptionCharge> charges, Long companyId,
            Long billingDocumentId) {
        List<Long> ids = charges.stream().map(SubscriptionCharge::getId).toList();
        int sellados = chargeRepository.sealAsInvoiced(ids, companyId, billingDocumentId);
        if (sellados != ids.size())
            throw new IllegalStateException("expected to seal " + ids.size()
                    + " charges into document " + billingDocumentId + " but sealed " + sellados
                    + ": another process billed some of them concurrently");
    }
}
