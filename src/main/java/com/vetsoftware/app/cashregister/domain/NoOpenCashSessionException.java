package com.vetsoftware.app.cashregister.domain;

/** No hay una sesión de caja OPEN en la sede y la empresa exige caja para cobrar (flag {@code cashregister.required}). */
public class NoOpenCashSessionException extends RuntimeException {
    public NoOpenCashSessionException(Long branchId) {
        super("No hay una caja abierta en la sede " + branchId + ". Abre la caja para cobrar.");
    }
}
