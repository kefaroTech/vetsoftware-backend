package com.vetsoftware.app.gatewaysettlement.domain;

/**
 * El lote ya esta atado a una entrada del extracto y no se reata.
 *
 * <p>
 * Mover un lote ya conciliado a otra linea del banco deja la primera cuadrada
 * contra nada, y <strong>ese descuadre no lo denuncia ninguna
 * constraint</strong>: la clave foranea sigue siendo perfectamente valida
 * apuntando a la nueva. El mensaje lleva el id de la entrada que ya estaba para
 * que quien lo intente pueda mirar cual de las dos es la correcta.
 */
public class BankReceiptAlreadyLinkedException extends RuntimeException {

    public BankReceiptAlreadyLinkedException(Long id, Long bankReceiptId) {
        super("Gateway settlement " + id + " is already linked to bank receipt " + bankReceiptId);
    }
}
