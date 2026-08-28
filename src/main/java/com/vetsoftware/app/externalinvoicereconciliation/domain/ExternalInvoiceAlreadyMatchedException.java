package com.vetsoftware.app.externalinvoicereconciliation.domain;

/**
 * Se intento registrar la factura del tercero sobre una conciliacion que ya la
 * tiene.
 *
 * <p>
 * <strong>Por que no se deja sobreescribir.</strong> La pareja externa es el
 * hecho contra el que se cuadro: cambiarla en sitio borraria el numero con el
 * que alguien ya explico un descuadre, y dejaria un {@code difference} que no
 * corresponde a ninguna factura conocida. Si la factura registrada era la
 * equivocada, lo que hay es un dato mal capturado y su correccion es una
 * decision con firma, no un segundo {@code POST}.
 *
 * <p>
 * Es un conflicto (409): el cuerpo es valido y lo que falla es el estado de la
 * conciliacion en este instante.
 */
public class ExternalInvoiceAlreadyMatchedException extends RuntimeException {

    public ExternalInvoiceAlreadyMatchedException(Long id,
            ExternalInvoiceReconciliationStatus status) {
        super("External invoice already matched for reconciliation " + id + ": status is "
                + status);
    }
}
