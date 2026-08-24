package com.vetsoftware.app.entitlement.domain;

/**
 * La empresa no tiene ningun contrato del que derivar permisos.
 *
 * <p>
 * No es un 404 sino un conflicto de estado: toda empresa nace con un contrato
 * en la misma transaccion (R10), asi que llegar aqui significa que hay una
 * cuenta corrupta. El recalculo <strong>falla ruidosamente y no toca la
 * tabla</strong>: vaciar los permisos de una empresa porque no encontramos su
 * contrato seria dejarla dentro del sistema sin poder hacer nada y sin ningun
 * mensaje que lo explique, que es el peor modo de fallo posible.
 */
public class CompanyWithoutContractException extends RuntimeException {

    public CompanyWithoutContractException(String message) {
        super(message);
    }
}
