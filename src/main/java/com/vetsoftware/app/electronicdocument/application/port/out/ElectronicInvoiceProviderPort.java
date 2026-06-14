package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;

/**
 * Puerto agnóstico de transmisión a la DIAN. Cada proveedor (Factus, MATIAS) lo implementa con un
 * adaptador. El caso de uso elige el adaptador por {@link #providerName()} contra el de la config.
 */
public interface ElectronicInvoiceProviderPort {
    /** Nombre del proveedor que atiende este adaptador (debe coincidir con ProviderConfigSnapshot.provider). */
    String providerName();

    /** Transmite el documento y devuelve el resultado normalizado (terminal o pendiente). */
    ProviderResult transmit(ElectronicDocument document, ProviderConfigSnapshot config);
}
