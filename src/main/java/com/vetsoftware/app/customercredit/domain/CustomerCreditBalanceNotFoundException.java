package com.vetsoftware.app.customercredit.domain;

/**
 * La empresa no tiene fila resumen todavia.
 *
 * <p>
 * No es lo mismo que «tiene cero»: la fila nace escribiendo su cero la primera
 * vez que se le concede saldo. Que no exista significa que a esta empresa nunca
 * se le abono nada, y distinguir las dos cosas es lo que evita que un cuadre
 * lea un cero inventado como si fuera un dato.
 */
public class CustomerCreditBalanceNotFoundException extends RuntimeException {
    public CustomerCreditBalanceNotFoundException(Long companyId) {
        super("CustomerCreditBalance not found for company: " + companyId);
    }
}
