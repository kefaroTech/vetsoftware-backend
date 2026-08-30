package com.vetsoftware.app.aiproposal.domain;

/**
 * Dos pestanas son dos clientes.
 *
 * <p>
 * {@code refine} y {@code PUT /lines} escriben sobre la misma propuesta desde
 * un token publico y <strong>sin sesion</strong>, asi que no hay nada que
 * serialice a los dos escritores salvo la version que los dos leyeron. Sin
 * esto, un refinamiento en vuelo pisa una edicion manual recien aplicada y
 * devuelve la linea que el usuario acababa de quitar -el fallo exacto que la
 * soberania de la edicion manual (plan S8.3) existe para prevenir-.
 *
 * <p>
 * El front recarga y reintenta. <strong>No lleva la version esperada en el
 * mensaje</strong>: quien no tiene el token tampoco tiene por que aprender nada
 * del estado de una propuesta ajena.
 */
public class ProposalVersionConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProposalVersionConflictException() {
        super("The proposal changed since it was read. Reload it and try again.");
    }
}
