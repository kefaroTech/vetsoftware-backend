package com.vetsoftware.app.dianprovider.domain;

/**
 * Proveedor tecnologico DIAN. La integracion es agnostica: se empieza con FACTUS (tiene sandbox,
 * validacion SINCRONA) y la meta es MATIAS (validacion ASINCRONA por webhooks).
 */
public enum ProviderType {
    FACTUS(false),
    MATIAS(true);

    private final boolean asynchronous;

    ProviderType(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    /** true si el resultado DIAN llega despues por webhook (no en la respuesta de transmision). */
    public boolean isAsynchronous() {
        return asynchronous;
    }
}
