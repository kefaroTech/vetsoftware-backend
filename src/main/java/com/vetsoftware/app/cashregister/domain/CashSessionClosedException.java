package com.vetsoftware.app.cashregister.domain;

/**
 * Se intentó operar (movimiento o cierre) sobre una sesión de caja que ya está
 * cerrada.
 */
public class CashSessionClosedException extends RuntimeException {
    public CashSessionClosedException(Long id) {
        super("La sesión de caja ya está cerrada: " + id);
    }
}
