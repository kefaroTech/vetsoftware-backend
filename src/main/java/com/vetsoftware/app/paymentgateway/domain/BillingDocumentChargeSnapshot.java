package com.vetsoftware.app.paymentgateway.domain;

import java.math.BigDecimal;

/**
 * El documento de cobro de otra feature ({@code subscriptionbilling}) visto
 * desde aquí: solo lo justo para decidir si hay algo que cobrar.
 *
 * <p>
 * {@code issueStatus} viaja como el nombre crudo del enum ajeno
 * ({@code IssueStatus.name()}) y no tipado: tiparlo aquí sería adivinar un
 * contrato que pertenece a {@code subscriptionbilling}.
 */
public record BillingDocumentChargeSnapshot(Long documentId, String documentNumber,
        BigDecimal totalAmount, BigDecimal balanceAmount, String currency, Long subscriptionId,
        String issueStatus) {
}
