package com.vetsoftware.app.dunning.domain;

import java.math.BigDecimal;

/**
 * Companion VO del documento que disparo el aviso. Es nulo en los eventos de
 * contrato ({@code READ_ONLY_APPLIED}, {@code REACTIVATED}), que no cuelgan de
 * ninguna factura concreta.
 */
public record BillingDocumentRef(Long id, Long companyId, String documentNumber,
        BigDecimal balanceAmount) {
    public BillingDocumentRef {
        if (id == null)
            throw new IllegalArgumentException("billing document id is required");
        if (companyId == null)
            throw new IllegalArgumentException("billing document company id is required");
    }
}
