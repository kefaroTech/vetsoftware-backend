package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.VoidBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.VoidBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocumentNotFoundException;
import io.micrometer.observation.annotation.Observed;
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

    private static final Logger log = LoggerFactory.getLogger(VoidBillingDocumentService.class);

    private final BillingDocumentRepository repository;
    private final SubscriptionChargeRepository chargeRepository;
    private final SubscriptionBillingMetrics metrics;
    private final SubscriptionBillingAuditPort audit;

    public VoidBillingDocumentService(BillingDocumentRepository repository,
            SubscriptionChargeRepository chargeRepository, SubscriptionBillingMetrics metrics,
            SubscriptionBillingAuditPort audit) {
        this.repository = repository;
        this.chargeRepository = chargeRepository;
        this.metrics = metrics;
        this.audit = audit;
    }

    @Override
    @Transactional
    public BillingDocumentDto execute(VoidBillingDocumentCommand command) {
        SubscriptionBillingDocument document = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionBillingDocumentNotFoundException(command.id()));
        document.voidDocument();
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
