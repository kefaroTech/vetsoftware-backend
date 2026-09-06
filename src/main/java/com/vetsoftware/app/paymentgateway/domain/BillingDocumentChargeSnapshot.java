package com.vetsoftware.app.paymentgateway.domain;

import java.math.BigDecimal;

/**
 * El documento de cobro de otra feature ({@code subscriptionbilling}) visto
 * desde aquí: solo lo justo para decidir si hay algo que cobrar.
 */
public record BillingDocumentChargeSnapshot(Long documentId, String documentNumber,
        BigDecimal totalAmount, BigDecimal balanceAmount, String currency, Long subscriptionId) {
}
