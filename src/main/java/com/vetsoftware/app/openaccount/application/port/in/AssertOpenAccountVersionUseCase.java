package com.vetsoftware.app.openaccount.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface AssertOpenAccountVersionUseCase {
  // Internal-only: never mapped to a REST endpoint. Se invoca como guard al inicio de una mutación
  // de cargo/abono/estado ya autorizada (vía el OpenAccountVersionGuard de cada feature), así que
  // no
  // impone autoridades openAccount.* al caller — solo exige principal autenticado, como
  // Recalculate.
  // No-op cuando expectedVersion es null (opt-in: el front decide cuándo enviar la versión).
  // Scoped a la empresa (companyId): la cuenta se resuelve dentro del tenant; una cuenta ajena
  // lanza
  // OpenAccountNotFoundException (defensa en profundidad, no filtra versión cross-tenant).
  @PreAuthorize("isAuthenticated()")
  void assertVersion(Long companyId, Long openAccountId, Long expectedVersion);
}
