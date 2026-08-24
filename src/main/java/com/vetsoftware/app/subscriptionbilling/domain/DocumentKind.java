package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Qué papel es el documento de cobro, y con ello qué significan sus importes.
 *
 * <p>
 * <b>Aquí vive la mitad «documento» de la convención de signos</b>
 * ({@code suscripciones-modelo.md} §3): los importes del documento son
 * <b>siempre positivos</b> ({@code chk_sbd_amounts_positive}) y el signo lo da
 * este tipo. Un papel con total negativo no existe: existe una nota crédito por
 * el valor absoluto. Guardarlo negativo obligaría a que
 * {@code total_amount = subtotal_amount + tax_amount} y
 * {@code settled_amount <= total_amount} cambiaran de sentido según el tipo,
 * que es exactamente el laberinto en el que una devolución dejó de caber en el
 * esquema.
 *
 * <p>
 * Espejo de {@code chk_sbd_kind}.
 */
public enum DocumentKind {
    /** Suma a la deuda del cliente. */
    INVOICE,
    /**
     * Resta. Sus importes se guardan <b>positivos</b>; el efecto negativo lo
     * produce la aplicación contra el documento destino, que vive en el slice
     * {@code subscriptionpayment}.
     */
    CREDIT_NOTE,
    /** Suma. */
    DEBIT_NOTE;

    /**
     * {@code true} si el tipo puede encadenarse al documento que corrige. Espejo de
     * {@code chk_sbd_corrects_kind}.
     */
    public boolean puedeCorregir() {
        return this == CREDIT_NOTE || this == DEBIT_NOTE;
    }

    /**
     * La serie del consecutivo interno que numera este tipo de documento:
     * {@code DC} cuentas de cobro, {@code NC} notas crédito, {@code ND} notas
     * débito.
     *
     * <p>
     * Son series <b>separadas</b> a propósito: mezclarlas en un solo contador haría
     * que el número no dijera qué papel es, que es justo lo que se le pide a un
     * consecutivo cuando alguien lo lee en un extracto.
     */
    public String sequencePrefix() {
        return switch (this) {
            case INVOICE -> "DC";
            case CREDIT_NOTE -> "NC";
            case DEBIT_NOTE -> "ND";
        };
    }

    /**
     * El signo que deben tener <b>todos</b> los cargos agrupados en un documento de
     * este tipo, o {@code null} si el tipo no lo restringe.
     *
     * <p>
     * Una nota crédito agrupa cargos que restan; mezclarle un cargo positivo hace
     * que el {@code ABS(SUM(...))} de la conciliación R6 deje de ser su subtotal, y
     * entonces <b>la vigilancia miente sin devolver ninguna fila</b>. Es la regla
     * derivada que la base no puede imponer ({@code suscripciones-modelo.md} §3.2),
     * y por eso está aquí y no en un {@code CHECK}.
     *
     * <p>
     * Una factura <b>sí</b> puede mezclar signos —una cuota positiva con su
     * descuento negativo es lo normal— y por eso {@code INVOICE} devuelve
     * {@code null}: lo que se le exige es que ningún grupo de tarifa quede en
     * negativo, que es cosa de {@link TaxBreakdown}.
     */
    public Integer signoExigidoALosCargos() {
        return this == CREDIT_NOTE ? -1 : null;
    }
}
