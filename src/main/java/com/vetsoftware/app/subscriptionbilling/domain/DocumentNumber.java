package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * El número interno de un documento de cobro: {@code DC-000001}.
 *
 * <p>
 * <b>No es el número de la factura fiscal.</b> Este lo genera Lumbre y existe
 * <b>desde que se calcula el cobro</b>, antes de que exista ninguna factura; el
 * fiscal lo pone el emisor externo y llega después, en
 * {@code external_invoice_number}. Ver {@link IssueStatus} para la distinción
 * completa entre los dos circuitos.
 */
public record DocumentNumber(String prefix, long value) {

    /** Ancho del consecutivo con ceros a la izquierda. */
    private static final int ANCHO = 6;

    /**
     * Longitud máxima de {@code subscription_billing_documents.document_number}.
     */
    public static final int MAX_LENGTH = 30;

    public DocumentNumber {
        if (prefix == null || prefix.isBlank())
            throw new IllegalArgumentException("prefix is required");
        if (prefix.length() > 10)
            throw new IllegalArgumentException("prefix must be 10 chars or less");
        if (value < 1)
            throw new IllegalArgumentException("sequence value must be greater than zero");
    }

    /**
     * El número tal como se imprime y tal como se guarda.
     *
     * <p>
     * Los ceros a la izquierda son cosmética con un efecto real: el consecutivo se
     * ordena por texto en cualquier informe y sin relleno el 10 va antes que el 9.
     * El ancho no acota nada —un valor de siete dígitos sale con siete— porque un
     * consecutivo que se corta al llegar al millón es un modo de fallo peor que un
     * número feo.
     */
    public String formatted() {
        String formatted = prefix + "-" + String.format("%0" + ANCHO + "d", value);
        if (formatted.length() > MAX_LENGTH)
            throw new IllegalArgumentException(
                    "document number exceeds " + MAX_LENGTH + " chars: " + formatted);
        return formatted;
    }
}
