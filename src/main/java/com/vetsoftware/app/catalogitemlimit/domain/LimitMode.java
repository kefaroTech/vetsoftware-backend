package com.vetsoftware.app.catalogitemlimit.domain;

/**
 * Si el artículo trae techo o no.
 *
 * <p>
 * La base exige cantidad si es {@code LIMITED} y la prohíbe si es {@code FULL}:
 * no cabe un techo a medio declarar.
 */
public enum LimitMode {
    FULL, LIMITED;

    public boolean requiresQuantity() {
        return this == LIMITED;
    }
}
