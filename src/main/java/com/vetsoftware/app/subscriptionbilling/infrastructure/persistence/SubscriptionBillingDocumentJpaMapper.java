package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentTax;
import com.vetsoftware.app.subscriptionbilling.domain.ExternalInvoiceReference;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * El único sitio que conoce a la vez el documento de dominio y su fila.
 *
 * <p>
 * <b>Aquí no aparece {@code balanceAmount} por ningún lado, y es la
 * barrera.</b> La entidad no tiene mutador para esa columna y el dominio la
 * deriva de {@code total - settled}, así que no existe ninguna línea de código
 * —ni un descuido futuro— capaz de escribir un saldo distinto del que calcula
 * la base. Es la columna que decide la mora: desincronizarla es suspender a
 * quien ya pagó.
 */
@Component
public class SubscriptionBillingDocumentJpaMapper {

    public SubscriptionBillingDocumentJpaEntity toJpa(SubscriptionBillingDocument document) {
        SubscriptionBillingDocumentJpaEntity entity = new SubscriptionBillingDocumentJpaEntity();
        entity.setId(document.getId());
        entity.setDocumentNumber(document.getDocumentNumber());
        entity.setCompanyId(document.getCompanyId());
        entity.setSubscriptionId(document.getSubscriptionId());
        entity.setDocumentKind(document.getDocumentKind());
        entity.setBillingReason(document.getBillingReason());
        entity.setPeriodStart(document.getPeriod().start());
        entity.setPeriodEnd(document.getPeriod().end());
        entity.setIssueStatus(document.getIssueStatus());
        ExternalInvoiceReference external = document.getExternal();
        entity.setExternalInvoiceNumber(external == null ? null : external.invoiceNumber());
        entity.setExternalCufe(external == null ? null : external.cufe());
        entity.setExternalIssuedAt(external == null ? null : external.issuedAt());
        entity.setExternalProvider(external == null ? null : external.provider());
        entity.setExternalRegisteredAt(external == null ? null : external.registeredAt());
        entity.setExternalRegisteredBySystemUserId(
                external == null ? null : external.registeredBySystemUserId());
        entity.setCorrectsDocumentId(document.getCorrectsDocumentId());
        entity.setDueDate(document.getDueDate());
        entity.setSubtotalAmount(document.getSubtotalAmount());
        entity.setTaxAmount(document.getTaxAmount());
        entity.setTotalAmount(document.getTotalAmount());
        entity.setSettledAmount(document.getSettledAmount());
        entity.setCreatedDate(document.getCreatedDate());
        entity.setVersion(document.getVersion());
        return entity;
    }

    /**
     * Reconstruye el documento con su desglose.
     *
     * <p>
     * La referencia externa se reconstruye <b>solo si hay número</b>: es lo que
     * {@code chk_sbd_external_registered} garantiza para un documento registrado, y
     * lo que permite que un {@code DRAFT} —sin nada de eso— vuelva del mapper sin
     * inventarse un VO a medio llenar.
     */
    public SubscriptionBillingDocument toDomain(SubscriptionBillingDocumentJpaEntity entity,
            List<BillingDocumentTax> taxes) {
        ExternalInvoiceReference external = entity.getExternalInvoiceNumber() == null
                ? null
                : new ExternalInvoiceReference(entity.getExternalInvoiceNumber(),
                        entity.getExternalCufe(), entity.getExternalIssuedAt(),
                        entity.getExternalProvider(), entity.getExternalRegisteredAt(),
                        entity.getExternalRegisteredBySystemUserId());
        return new SubscriptionBillingDocument(entity.getId(), entity.getDocumentNumber(),
                entity.getCompanyId(), entity.getSubscriptionId(), entity.getDocumentKind(),
                entity.getBillingReason(),
                new ServicePeriod(entity.getPeriodStart(), entity.getPeriodEnd()),
                entity.getIssueStatus(), external, entity.getCorrectsDocumentId(),
                entity.getDueDate(), entity.getSubtotalAmount(), entity.getTaxAmount(),
                entity.getTotalAmount(), entity.getSettledAmount(), taxes, entity.getCreatedDate(),
                entity.getVersion());
    }
}
