package com.vetsoftware.app.cashregister.domain;

/**
 * El empleado ya es responsable de otra sesión OPEN, sin importar su sede o
 * terminal.
 */
public class EmployeeCashSessionAlreadyOpenException extends RuntimeException {

    public EmployeeCashSessionAlreadyOpenException() {
        super("Ya tienes una caja abierta. Debes cerrarla antes de abrir otra.");
    }
}
