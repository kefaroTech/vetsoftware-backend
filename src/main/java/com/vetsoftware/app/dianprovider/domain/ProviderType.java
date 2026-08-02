package com.vetsoftware.app.dianprovider.domain;

/**
 * Proveedor tecnologico DIAN. Por ahora solo MATIAS (UBL 2.1; el documento se
 * transmite y el estado DIAN se consulta luego). El puerto sigue siendo
 * agnostico por si se agregan mas proveedores en el futuro.
 */
public enum ProviderType {
    MATIAS(true);

    private final boolean asynchronous;

    ProviderType(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    /**
     * true si el resultado DIAN llega despues por webhook (no en la respuesta de
     * transmision).
     */
    public boolean isAsynchronous() {
        return asynchronous;
    }
}
