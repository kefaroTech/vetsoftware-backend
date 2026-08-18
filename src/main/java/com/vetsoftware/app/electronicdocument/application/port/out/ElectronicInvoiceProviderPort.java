package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import java.util.Optional;

/**
 * Puerto agnóstico de transmisión a la DIAN. Cada proveedor (hoy MATIAS) lo
 * implementa con un adaptador. El caso de uso elige el adaptador por
 * {@link #providerName()} contra el de la config.
 */
public interface ElectronicInvoiceProviderPort {
    /**
     * Nombre del proveedor que atiende este adaptador (debe coincidir con
     * ProviderConfigSnapshot.provider).
     */
    String providerName();

    /**
     * Transmite el documento y devuelve el resultado normalizado (terminal o
     * pendiente).
     */
    ProviderResult transmit(ElectronicDocument document, ProviderConfigSnapshot config);

    /**
     * Consulta el estado actual de un documento ya transmitido, para reconciliar
     * los PENDIENTE cuando el webhook de un proveedor asíncrono se pierde. Devuelve
     * el {@link ProviderResult} normalizado, o vacío si el proveedor no soporta
     * polling (resuelve ya en {@link #transmit}).
     *
     * <p>
     * Recibe el {@code document} —no solo su clave— porque el sello fiscal que
     * devuelve la reconciliación depende del <b>tipo</b>: una factura de venta
     * sella CUFE y un documento equivalente POS o una nota sellan CUDE. La
     * respuesta del proveedor no trae el tipo, así que sin este parámetro el
     * adaptador tendría que asumir uno (defecto real: todo lo reconciliado se
     * guardaba como CUFE).
     */
    default Optional<ProviderResult> fetchStatus(ElectronicDocument document,
            String providerDocumentKey, ProviderConfigSnapshot config) {
        return Optional.empty();
    }
}
