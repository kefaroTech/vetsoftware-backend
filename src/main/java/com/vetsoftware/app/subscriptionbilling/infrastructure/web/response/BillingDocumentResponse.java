package com.vetsoftware.app.subscriptionbilling.infrastructure.web.response;

import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * La cuenta de cobro tal como la ven los frontends.
 *
 * <p>
 * {@code documentNumber} es el número <b>interno</b> ({@code DC-…}) y
 * {@code externalInvoiceNumber} el de la factura fiscal emitida fuera. Los dos
 * viajan juntos a propósito: el {@code DC} va impreso en la factura externa y
 * es lo que permite emparejarlas sin adivinar.
 *
 * <p>
 * {@code balanceAmount} es el saldo, derivado de {@code total − settled}.
 * Ningún camino de código lo escribe.
 */
public record BillingDocumentResponse(Long id, Long companyId, String documentNumber,
        Long subscriptionId, DocumentKind documentKind, BillingReason billingReason,
        LocalDate periodStart, LocalDate periodEnd, IssueStatus issueStatus,
        String externalInvoiceNumber, String externalCufe, LocalDate externalIssuedAt,
        String externalProvider, LocalDateTime externalRegisteredAt,
        Long externalRegisteredBySystemUserId, Long correctsDocumentId, LocalDate dueDate,
        BigDecimal subtotalAmount, BigDecimal taxAmount, BigDecimal totalAmount,
        BigDecimal settledAmount, BigDecimal balanceAmount, List<BillingDocumentTaxSummary> taxes,
        LocalDateTime createdDate, Long version) {

    public static BillingDocumentResponse from(BillingDocumentDto dto) {
        return new BillingDocumentResponse(dto.id(), dto.companyId(), dto.documentNumber(),
                dto.subscriptionId(), dto.documentKind(), dto.billingReason(), dto.periodStart(),
                dto.periodEnd(), dto.issueStatus(), dto.externalInvoiceNumber(), dto.externalCufe(),
                dto.externalIssuedAt(), dto.externalProvider(), dto.externalRegisteredAt(),
                dto.externalRegisteredBySystemUserId(), dto.correctsDocumentId(), dto.dueDate(),
                dto.subtotalAmount(), dto.taxAmount(), dto.totalAmount(), dto.settledAmount(),
                dto.balanceAmount(),
                dto.taxes().stream().map(BillingDocumentTaxSummary::from).toList(),
                dto.createdDate(), dto.version());
    }
}
