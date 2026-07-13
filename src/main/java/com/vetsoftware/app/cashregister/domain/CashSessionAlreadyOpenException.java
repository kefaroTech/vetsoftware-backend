package com.vetsoftware.app.cashregister.domain;

/** Ya existe una sesión de caja OPEN para ese (empresa, sede, terminal): no se puede abrir otra. */
public class CashSessionAlreadyOpenException extends RuntimeException {
    public CashSessionAlreadyOpenException(Long branchId, String terminal) {
        super("Ya hay una caja abierta en la sede " + branchId + " (terminal '" + terminal + "').");
    }
}
