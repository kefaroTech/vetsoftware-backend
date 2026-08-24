package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Dónde está el documento en el circuito «calcular aquí → emitir fuera →
 * registrar la referencia aquí».
 *
 * <p>
 * <b>La distinción que no se puede confundir, y que este javadoc existe para
 * dejar por escrito.</b> El motor de facturación electrónica DIAN <b>sigue
 * siendo parte del producto</b>: es lo que usan las clínicas para facturarle a
 * los dueños de las mascotas, con su propia resolución y su propia numeración,
 * y vive en el slice {@code electronicdocument} ({@code electronic_documents},
 * {@code numbering_resolutions}). Lo único que queda fuera del software es la
 * factura que <b>la plataforma le emite a la clínica</b>: esa se emite fuera y
 * aquí solo se guarda su referencia ({@code external_invoice_number},
 * {@code external_cufe}, {@code external_issued_at},
 * {@code external_provider}).
 *
 * <p>
 * <b>Dos emisores, dos numeraciones, dos tablas.</b> Nomenclatura fijada:
 * {@code DC-} el documento de cobro que genera VetSoftware · {@code FE-} la
 * factura fiscal del sistema externo · {@code NC-} la nota crédito externa. El
 * número {@code DC} viaja impreso en la factura externa: es lo que permite
 * emparejarlas después sin adivinar. El día que alguien mezcle los dos
 * circuitos, la contabilidad de los clientes y la propia quedan enredadas.
 *
 * <p>
 * Espejo de {@code chk_sbd_issue_status}.
 */
public enum IssueStatus {
    /** Calculado aquí. Tiene número {@code DC-} y todavía no existe fuera. */
    DRAFT,
    /**
     * Pendiente de emitirse fuera. <b>Los atascados aquí son la lista de trabajo
     * pendiente de cada mes</b>: cada fila es dinero devengado que nadie facturó.
     */
    AWAITING_EXTERNAL,
    /**
     * Emitido fuera y con la referencia capturada aquí. A partir de este estado el
     * documento <b>no cambia de importe</b> (R2): solo cambian lo saldado, el saldo
     * —que es columna calculada— y el propio estado. Corregirlo exige una nota
     * crédito emitida fuera y registrada aquí, encadenada al original por
     * {@code corrects_document_id}.
     */
    EXTERNAL_REGISTERED,
    /** Anulado antes de existir fuera. */
    VOIDED;

    /**
     * {@code true} si ya hay una factura fiscal detrás y el importe está sellado.
     */
    public boolean estaSelladoPorLaFacturaExterna() {
        return this == EXTERNAL_REGISTERED;
    }
}
