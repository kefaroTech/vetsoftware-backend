package com.vetsoftware.app.uvtvalue.domain;

/**
 * Ya hay UVT para ese ano. Espejo de {@code uq_uvt_values_year}. Mapea a 409.
 */
public class UvtValueAlreadyExistsException extends RuntimeException {

    public UvtValueAlreadyExistsException(int fiscalYear) {
        super("UVT value already published for fiscal year: " + fiscalYear);
    }
}
