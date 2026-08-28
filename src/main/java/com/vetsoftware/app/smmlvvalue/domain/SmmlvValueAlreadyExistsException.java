package com.vetsoftware.app.smmlvvalue.domain;

/**
 * Ya hay salario minimo para ese ano. Espejo de {@code uq_smmlv_values_year}.
 * 409.
 */
public class SmmlvValueAlreadyExistsException extends RuntimeException {

    public SmmlvValueAlreadyExistsException(int fiscalYear) {
        super("SMMLV value already published for fiscal year: " + fiscalYear);
    }
}
