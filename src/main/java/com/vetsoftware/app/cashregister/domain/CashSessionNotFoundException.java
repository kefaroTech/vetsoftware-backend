package com.vetsoftware.app.cashregister.domain;

/**
 * La sesión de caja pedida no existe (o no pertenece a la empresa del
 * solicitante).
 */
public class CashSessionNotFoundException extends RuntimeException {
    public CashSessionNotFoundException(Long id) {
        super("Sesión de caja no encontrada: " + id);
    }
}
