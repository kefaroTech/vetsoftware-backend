package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import java.util.Optional;

/**
 * Puerto agnóstico de transmisión a la DIAN. Cada proveedor (hoy MATIAS) lo implementa con un
 * adaptador. El caso de uso elige el adaptador por {@link #providerName()} contra el de la config.
 */
public interface ElectronicInvoiceProviderPort {
  /**
   * Nombre del proveedor que atiende este adaptador (debe coincidir con
   * ProviderConfigSnapshot.provider).
   */
  String providerName();

  /** Transmite el documento y devuelve el resultado normalizado (terminal o pendiente). */
  ProviderResult transmit(ElectronicDocument document, ProviderConfigSnapshot config);

  /**
   * Consulta el estado actual de un documento ya transmitido, para reconciliar los PENDIENTE cuando
   * el webhook de un proveedor asíncrono se pierde. Devuelve el {@link ProviderResult} normalizado,
   * o vacío si el proveedor no soporta polling (resuelve ya en {@link #transmit}).
   */
  default Optional<ProviderResult> fetchStatus(
      String providerDocumentKey, ProviderConfigSnapshot config) {
    return Optional.empty();
  }
}
