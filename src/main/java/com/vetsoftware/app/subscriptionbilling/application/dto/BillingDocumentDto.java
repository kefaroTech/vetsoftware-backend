package com.vetsoftware.app.subscriptionbilling.application.dto;

import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.ExternalInvoiceReference;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * La cuenta de cobro tal como sale de la aplicación.
 *
 * <p>
 * {@code balanceAmount} viaja <b>derivado</b> de {@code total - settled}, que
 * es la misma fórmula de la columna calculada de la base. No hay ningún camino
 * que lo escriba, aquí tampoco.
 *
 * <p>
 * Los cuatro campos {@code external*} son la referencia de la factura emitida
 * fuera —no la factura DIAN que la clínica le emite a sus clientes, que vive en
 * otro slice y en otra numeración—.
 */
public record BillingDocumentDto(Long id, String documentNumber, Long companyId,
        Long subscriptionId, DocumentKind documentKind, BillingReason billingReason,
        LocalDate periodStart, LocalDate periodEnd, IssueStatus issueStatus,
        String externalInvoiceNumber, String externalCufe, LocalDate externalIssuedAt,
        String externalProvider, LocalDateTime externalRegisteredAt,
        Long externalRegisteredBySystemUserId, Long correctsDocumentId, LocalDate dueDate,
        BigDecimal subtotalAmount, BigDecimal taxAmount, BigDecimal totalAmount,
        BigDecimal settledAmount, BigDecimal balanceAmount, List<BillingDocumentTaxDto> taxes,
        LocalDateTime createdDate, Long version) {

    public static BillingDocumentDto from(SubscriptionBillingDocument document) {
        ExternalInvoiceReference external = document.getExternal();
        return new BillingDocumentDto(document.getId(), document.getDocumentNumber(),
                document.getCompanyId(), document.getSubscriptionId(), document.getDocumentKind(),
                document.getBillingReason(), document.getPeriod().start(),
                document.getPeriod().end(), document.getIssueStatus(),
                external == null ? null : external.invoiceNumber(),
                external == null ? null : external.cufe(),
                external == null ? null : external.issuedAt(),
                external == null ? null : external.provider(),
                external == null ? null : external.registeredAt(),
                external == null ? null : external.registeredBySystemUserId(),
                document.getCorrectsDocumentId(), document.getDueDate(),
                document.getSubtotalAmount(), document.getTaxAmount(), document.getTotalAmount(),
                document.getSettledAmount(), document.getBalanceAmount(),
                document.getTaxes().stream().map(BillingDocumentTaxDto::from).toList(),
                document.getCreatedDate(), document.getVersion());
    }
}
