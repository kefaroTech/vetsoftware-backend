package com.vetsoftware.app.subscriptionpayment.domain;

import java.math.BigDecimal;

/**
 * Companion VO del documento de cobro que vive en {@code subscriptionbilling}.
 *
 * <p>
 * Es una <strong>copia</strong> de los datos que esta feature necesita, no una
 * referencia al agregado ajeno: el vertical slicing prohibe importar el dominio
 * de otra feature, y el puerto que lo resuelve
 * ({@code BillingDocumentQueryPort}) es el unico punto que conoce la otra
 * rodaja.
 *
 * <p>
 * <strong>{@code companyId} no es decorativo.</strong> Es la mitad de la clave
 * compuesta {@code (company_id, id)} con la que la base impide que un pago de
 * una clinica salde la factura de otra, y el constructor la exige para que un
 * {@code Ref} construido a mano no pueda saltarse la comprobacion que la
 * entidad hace despues.
 *
 * @param totalAmount
 *            total del documento, siempre positivo: el signo lo da
 *            {@code documentKind}, segun la convencion de signos del modelo
 * @param balanceAmount
 *            saldo pendiente. Es una <strong>columna calculada</strong> que
 *            mantiene la base ({@code total_amount - settled_amount}): este
 *            slice la lee y no la escribe nunca
 */
public record BillingDocumentRef(Long id, Long companyId, String documentNumber,
        String documentKind, BigDecimal totalAmount, BigDecimal balanceAmount) {

    /** Valor de {@code document_kind} que identifica una nota credito. */
    public static final String CREDIT_NOTE_KIND = "CREDIT_NOTE";

    public BillingDocumentRef {
        if (id == null)
            throw new IllegalArgumentException("billing document id is required");
        if (companyId == null)
            throw new IllegalArgumentException("billing document company id is required");
        if (documentKind == null || documentKind.isBlank())
            throw new IllegalArgumentException("billing document kind is required");
        if (totalAmount == null)
            throw new IllegalArgumentException("billing document total amount is required");
        if (totalAmount.signum() < 0)
            throw new IllegalArgumentException("billing document total amount cannot be negative");
    }

    public boolean isCreditNote() {
        return CREDIT_NOTE_KIND.equals(documentKind);
    }
}
