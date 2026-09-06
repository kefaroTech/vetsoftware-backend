package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.GenerateBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.command.IssueSubscriptionPeriodDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.dto.IssuedPeriodDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.GenerateBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.IssueSubscriptionPeriodDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillableSubscriptionItemPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.BillableSubscriptionItem;
import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.DuplicateBillingCycleException;
import com.vetsoftware.app.subscriptionbilling.domain.EmptyBillingDocumentException;
import com.vetsoftware.app.subscriptionbilling.domain.RecurringChargeKey;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Devenga y emite la cuenta de cobro de un contrato para un periodo exacto, sin
 * mover su calendario de facturación.
 *
 * <p>
 * Dos llamadores comparten este cuerpo:
 * {@code RunSubscriptionBillingCycleService}, que además avanza el periodo del
 * contrato tras emitir, y el cobro anticipado del primer periodo desde la
 * pasarela de pago, que no puede avanzar un calendario que el contrato todavía
 * no tiene.
 *
 * <p>
 * Las dos excepciones de {@link GenerateBillingDocumentUseCase} son desenlaces
 * normales aquí también, y nunca se propagan: un periodo ya facturado devuelve
 * el documento existente, y un periodo sin cargos pendientes devuelve
 * {@code issued = false} sin documento.
 */
@Observed(name = "subscription.billing.period.document.issue")
@Service
public class IssueSubscriptionPeriodDocumentService
        implements
            IssueSubscriptionPeriodDocumentUseCase {

    private static final Logger log = LoggerFactory
            .getLogger(IssueSubscriptionPeriodDocumentService.class);

    // El documento de cobro no modela todavia la divisa: ver
    // IssuedPeriodDocumentDto.
    private static final String CURRENCY = "COP";

    private final BillableSubscriptionItemPort itemPort;
    private final SubscriptionChargeRepository chargeRepository;
    private final GenerateBillingDocumentUseCase generateUseCase;
    private final BillingDocumentRepository documentRepository;
    private final Clock clock;

    public IssueSubscriptionPeriodDocumentService(BillableSubscriptionItemPort itemPort,
            SubscriptionChargeRepository chargeRepository,
            GenerateBillingDocumentUseCase generateUseCase,
            BillingDocumentRepository documentRepository, Clock clock) {
        this.itemPort = itemPort;
        this.chargeRepository = chargeRepository;
        this.generateUseCase = generateUseCase;
        this.documentRepository = documentRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IssuedPeriodDocumentDto execute(IssueSubscriptionPeriodDocumentCommand command) {
        ServicePeriod period = new ServicePeriod(command.periodStart(), command.periodEnd());
        int accruedCharges = accrue(command.companyId(), command.subscriptionId(), period);
        try {
            BillingDocumentDto document = generateUseCase
                    .execute(new GenerateBillingDocumentCommand(command.companyId(),
                            command.subscriptionId(), BillingReason.RECURRING_CYCLE, period.start(),
                            period.end()));
            return new IssuedPeriodDocumentDto(document.id(), document.documentNumber(),
                    document.totalAmount(), CURRENCY, true, accruedCharges);
        } catch (DuplicateBillingCycleException alreadyBilled) {
            log.debug("Periodo ya facturado para el contrato {}: {}", command.subscriptionId(),
                    alreadyBilled.getMessage());
            return documentoYaEmitido(command, accruedCharges);
        } catch (EmptyBillingDocumentException nothingToBill) {
            log.debug("Contrato {} sin cargos pendientes en {}..{}", command.subscriptionId(),
                    period.start(), period.end());
            return new IssuedPeriodDocumentDto(null, null, BigDecimal.ZERO, CURRENCY, false,
                    accruedCharges);
        }
    }

    private IssuedPeriodDocumentDto documentoYaEmitido(
            IssueSubscriptionPeriodDocumentCommand command, int accruedCharges) {
        SubscriptionBillingDocument existing = documentRepository
                .findRecurringCycleDocument(command.companyId(), command.subscriptionId(),
                        command.periodStart(), command.periodEnd())
                .orElseThrow(() -> new IllegalStateException(
                        "DuplicateBillingCycleException fired for subscription "
                                + command.subscriptionId()
                                + " but no matching document was found"));
        return new IssuedPeriodDocumentDto(existing.getId(), existing.getDocumentNumber(),
                existing.getTotalAmount(), CURRENCY, false, accruedCharges);
    }

    /**
     * Devenga una linea por cada linea del contrato que cobra y que todavia no
     * tiene su cargo de este periodo.
     *
     * <p>
     * La comprobacion de {@link RecurringChargeKey} <b>no filtra por estado del
     * cargo</b>: un cargo ya facturado sigue bloqueando el duplicado, que es justo
     * lo que hace falta cuando el llamador se reinicia despues de haber emitido la
     * factura.
     */
    private int accrue(Long companyId, Long subscriptionId, ServicePeriod period) {
        List<BillableSubscriptionItem> items = itemPort.findCurrentOn(companyId, subscriptionId,
                period.start());
        int accrued = 0;
        for (BillableSubscriptionItem item : items) {
            if (!item.devenga(period.start()))
                continue;
            int billable = item.billableQuantity();
            // Todo dentro de lo incluido: no hay cargo, y no es lo mismo que un cargo de
            // cero -SubscriptionCharge exige cantidad positiva-.
            if (billable == 0)
                continue;
            RecurringChargeKey key = RecurringChargeKey.of(companyId, subscriptionId, item.id(),
                    period);
            if (chargeRepository.existsRecurringCharge(key)) {
                log.debug("Cargo recurrente ya devengado, se omite: {}", key.value());
                continue;
            }
            chargeRepository.save(SubscriptionCharge.create(companyId, subscriptionId, item.id(),
                    ChargeType.RECURRING, item.itemName(), period, BigDecimal.valueOf(billable),
                    item.unitAmount(), item.recurringSubtotal(), item.taxRate(),
                    item.taxTreatment(), null, null, clock));
            accrued++;
        }
        return accrued;
    }
}
