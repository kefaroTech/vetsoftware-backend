package com.vetsoftware.app.subscriptionpayment.domain;

/**
 * De donde sale cada peso que baja el saldo de una factura. Espejo exacto de
 * {@code chk_bda_source_kind} (changeset 253): si aqui aparece un valor que la
 * constraint no admite, el {@code INSERT} lo rechaza la base y el fallo llega
 * como un 409 sin explicacion.
 *
 * <p>
 * <strong>Son seis, y los cuatro ultimos saldan SIN QUE ENTRE DINERO.</strong>
 * Esa es la idea que la primera version del modelo no tenia y que motiva la
 * capa K entera. El caso: Ana debe 213.010, su contadora le practica retencion
 * en la fuente y le gira 205.850. Con solo {@link #PAYMENT} y
 * {@link #CREDIT_NOTE} quedan 7.160 vivos, arranca la mora, se agotan los cinco
 * dias de gracia y la clinica cae a solo lectura <em>por una deuda que
 * fiscalmente no existe</em>: ese dinero esta en la DIAN a nombre de
 * VetSoftware y Ana tiene el certificado que lo prueba.
 *
 * <p>
 * <strong>La comision de la pasarela NO esta en esta lista y no debe
 * estarlo.</strong> El cliente paga 100.000 y al banco entran 96.800, pero esos
 * 3.200 son gasto propio, no un menor pago del cliente. La factura se salda por
 * el BRUTO y la comision vive en {@code subscription_payments.fee_amount} y en
 * {@code gateway_settlements}. Meterla aqui haria que la cartera cuadrara con
 * el banco y dejara de cuadrar con lo facturado, que es peor.
 */
public enum ApplicationSourceKind {

    /** Un pago recibido: entro dinero. Apunta a {@code payment_id}. */
    PAYMENT,

    /**
     * Un saldo a favor de una nota credito: no entra un peso y salda igual. Apunta
     * a {@code source_document_id}.
     */
    CREDIT_NOTE,

    /**
     * La retencion que te practico el cliente. No es un descuento ni un impago: es
     * plata tuya que fue directa a la DIAN, y solo se puede imputar en tu
     * declaracion si tienes el certificado. Apunta a {@code withholding_id}
     * ({@code document_withholdings}), donde vive el detalle fiscal -tipo, base,
     * tarifa, municipio, periodo y certificado-.
     */
    WITHHOLDING,

    /**
     * Un saldo a favor del cliente que se aplica a una factura. Apunta a
     * {@code credit_entry_id} ({@code customer_credit_entries}) y es el camino de
     * SALIDA del saldo a favor: sin este origen el credito nacia y no podia
     * aplicarse nunca. Se consume por lotes, primero lo que antes caduca, y cada
     * consumo anota de que lote salio.
     */
    CUSTOMER_CREDIT,

    /**
     * El residuo de uno a tres pesos entre el impuesto calculado aqui y el del
     * emisor externo. Sin este origen ese residuo queda vivo para siempre y el
     * saldo nunca llega a cero. Con tope duro en la base
     * ({@code chk_bda_rounding_cap}), para que no se convierta en el vertedero
     * donde cuadra cualquier descuadre.
     */
    ROUNDING,

    /**
     * Se dio por incobrable. Exige firma y motivo
     * ({@code chk_bda_write_off_signature}): la deuda no se borra, se agrega un
     * documento que la da de baja, y los dos quedan.
     */
    WRITE_OFF;

    /** Si el origen es dinero que entro de verdad a la cuenta. */
    public boolean isCashInflow() {
        return this == PAYMENT;
    }

    /** Si el origen exige firma nominal de plataforma para escribirse. */
    public boolean requiresPlatformSignature() {
        return this == WRITE_OFF;
    }
}
