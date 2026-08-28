package com.vetsoftware.app.customercredit.domain;

/**
 * De donde sale el asiento. Espejo exacto de {@code chk_cce_origin_kind}.
 *
 * <p>
 * <strong>El origen lleva clave foranea real por rama, no un identificador
 * suelto</strong>: un asiento con origen huerfano es un pasivo que nadie puede
 * probar. Tres ramas apuntan a algo —pago en exceso, nota credito sobre factura
 * saldada, baja con periodo pagado por delante— y una cuarta al documento al
 * que se aplico. El redondeo, la caducidad y la correccion manual no apuntan a
 * nada, y {@code chk_cce_origin_branch} exige que sus tres columnas vayan
 * vacias.
 */
public enum CreditOriginKind {

    /** Pago en exceso: apunta a {@code subscription_payments}. */
    OVERPAYMENT,

    /** Nota credito sobre factura saldada: apunta al documento de cobro. */
    CREDIT_NOTE,

    /** Baja con periodo pagado por delante: apunta a la suscripcion. */
    CANCELLATION,

    /** Aplicacion del saldo a un documento: apunta al documento. */
    APPLICATION,

    /** Caducidad del remanente de un lote. Sin origen documental. */
    EXPIRY,

    /** Redondeo. Sin origen documental. */
    ROUNDING,

    /** Correccion manual. Sin origen documental. */
    MANUAL;

    /** La rama que apunta a un pago. */
    public boolean pointsToPayment() {
        return this == OVERPAYMENT;
    }

    /** Las dos ramas que apuntan a un documento de cobro. */
    public boolean pointsToDocument() {
        return this == CREDIT_NOTE || this == APPLICATION;
    }

    /** La rama que apunta a una suscripcion. */
    public boolean pointsToSubscription() {
        return this == CANCELLATION;
    }

    /** Las tres ramas sin origen documental. */
    public boolean pointsToNothing() {
        return this == EXPIRY || this == ROUNDING || this == MANUAL;
    }
}
