package com.vetsoftware.app.subscription.application.dto;

import java.math.BigDecimal;

/**
 * El documento del primer periodo, tal como
 * {@link com.vetsoftware.app.subscription.application.port.out.SubscriptionPeriodDocumentIssuerPort}
 * lo deja ver desde {@code subscription}.
 *
 * @param totalAmount
 *            lo que el documento cobra en total
 */
public record IssuedPeriodDocument(Long documentId, BigDecimal totalAmount) {
}
