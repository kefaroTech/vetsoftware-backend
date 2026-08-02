package com.vetsoftware.app.debtopenaccount.application.port.out;

/**
 * Detección temprana de conflicto: valida que la versión esperada por el front coincida con la
 * actual de la cuenta antes de mutar. Delega en el caso de uso de openaccount (mismo patrón que
 * {@link OpenAccountRefresher}). No-op cuando expectedVersion es null.
 */
public interface OpenAccountVersionGuard {
  void assertVersion(Long companyId, Long openAccountId, Long expectedVersion);
}
