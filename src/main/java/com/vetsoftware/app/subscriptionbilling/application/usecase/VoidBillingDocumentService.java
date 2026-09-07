package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.VoidBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.VoidBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentApplicationReversalPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.PendingPaymentApplicationQueryPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentHasPendingPaymentException;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocumentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anula un documento que todavía no existe fuera.
 *
 * <p>
 * Uno con factura externa ya registrada <b>no</b> se anula aquí: se corrige con
 * una nota crédito encadenada. Lo rechaza el propio agregado, no este servicio,
 * para que la regla siga valiendo desde cualquier otro caller.
 *
 * <p>
 * <b>Anular libera los cargos que el documento tenía sellados</b>, y las dos
 * cosas van en la misma transacción. Sin eso quedaban en {@code INVOICED}
 * apuntando a un documento {@code VOIDED}: el ciclo siguiente no los recoge
 * —{@code findPendingByCompanyIdAndSubscription} filtra {@code PENDING}— y no
 * hay ninguna vigilancia que los detecte, así que eran dinero devengado que no
 * se facturaba nunca y sin una sola señal. Si la liberación fallara, la
 * anulación revierte con ella: un documento anulado con sus cargos todavía
 * atados es exactamente el estado que este servicio existe para no dejar.
 */
@Observed(name = "subscription.billing.document.void")
@Service
public class VoidBillingDocumentService implements VoidBillingDocumentUseCase {

    /**
     * El motivo que queda en el reverso automatico. No es el motivo de la anulacion
     * en si -{@code VoidBillingDocumentCommand} no lo pide (ver el
     * {@code audit.documentVoided} de abajo)-, es el de la reversion que la
     * anulacion arrastra.
     */
    private static final String REVERSAL_REASON = "documento anulado";

    private static final Logger log = LoggerFactory.getLogger(VoidBillingDocumentService.class);

    private final BillingDocumentRepository repository;
    private final SubscriptionChargeRepository chargeRepository;
    private final SubscriptionBillingMetrics metrics;
    private final SubscriptionBillingAuditPort audit;
    private final PendingPaymentApplicationQueryPort pendingPaymentApplicationQueryPort;
    private final BillingDocumentApplicationReversalPort reversalPort;

    public VoidBillingDocumentService(BillingDocumentRepository repository,
            SubscriptionChargeRepository chargeRepository, SubscriptionBillingMetrics metrics,
            SubscriptionBillingAuditPort audit,
            PendingPaymentApplicationQueryPort pendingPaymentApplicationQueryPort,
            BillingDocumentApplicationReversalPort reversalPort) {
        this.repository = repository;
        this.chargeRepository = chargeRepository;
        this.metrics = metrics;
        this.audit = audit;
        this.pendingPaymentApplicationQueryPort = pendingPaymentApplicationQueryPort;
        this.reversalPort = reversalPort;
    }

    @Override
    @Transactional
    public BillingDocumentDto execute(VoidBillingDocumentCommand command) {
        SubscriptionBillingDocument document = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionBillingDocumentNotFoundException(command.id()));

        document.voidDocument();

        // Un pago PENDING aplicado todavia puede confirmarse o rechazarse; anular
        // ahora dejaria esa resolucion apuntando a un documento que el cliente ya no
        // puede ver. Va despues de la guardia del agregado: un documento que la
        // anulacion ya rechaza por su propio estado no necesita esta consulta.
        if (pendingPaymentApplicationQueryPort.existsPendingApplication(command.companyId(),
                document.getId())) {
            throw new BillingDocumentHasPendingPaymentException(document.getId());
        }
        // Un pago CONFIRMED ya salda de verdad y no bloquea la anulacion, pero no
        // puede quedar colgado de un documento anulado: se revierte antes de
        // guardar, para que un fallo aqui deje el documento donde estaba.
        List<Long> confirmedApplicationIds = pendingPaymentApplicationQueryPort
                .findConfirmedApplicationIds(command.companyId(), document.getId());
        for (Long applicationId : confirmedApplicationIds) {
            reversalPort.reverse(applicationId, command.companyId(), REVERSAL_REASON);
        }
        if (!confirmedApplicationIds.isEmpty()) {
            log.info("Documento {} anulado: {} aplicacion(es) de pago CONFIRMED revertidas",
                    document.getDocumentNumber(), confirmedApplicationIds.size());
        }

        SubscriptionBillingDocument saved = repository.save(document);

        // Los cargos que este documento sello vuelven a estar disponibles para el
        // ciclo siguiente. Va DESPUES del save y dentro de la misma transaccion: el
        // documento decide si la anulacion es legal -un documento ya emitido fuera la
        // rechaza-, y solo entonces tiene sentido soltar sus cargos.
        int liberados = chargeRepository.releaseFromVoidedDocument(saved.getId(),
                command.companyId());
        if (liberados > 0) {
            log.info("Documento {} anulado: {} cargo(s) devueltos a PENDING",
                    saved.getDocumentNumber(), liberados);
        }

        metrics.documentVoided(saved.getIssueStatus());
        // Sin motivo: VoidBillingDocumentCommand no lo pide, y este bloque no cambia el
        // contrato de la API para rellenar un campo. Que el «por que» falte queda dicho
        // aqui en vez de inventado; anadirlo es un cambio de request, no de telemetria.
        audit.documentVoided(saved.getId(), saved.getDocumentNumber(), saved.getSubscriptionId(),
                null);
        return BillingDocumentDto.from(saved);
    }
}
