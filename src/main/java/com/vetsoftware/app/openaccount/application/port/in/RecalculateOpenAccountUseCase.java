package com.vetsoftware.app.openaccount.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface RecalculateOpenAccountUseCase {
  // Internal-only: never mapped to a REST endpoint. Triggered as a side-effect of an
  // already-authorized charge/payment mutation (via each child's OpenAccountRefresher),
  // so it must not impose openAccount.* authorities on the charge caller — only require
  // an authenticated principal.
  // Scoped a la empresa (companyId): el lock/recálculo solo aplica a una cuenta de ese tenant;
  // una cuenta ajena lanza OpenAccountNotFoundException sin tomar el lock (defensa en profundidad).
  @PreAuthorize("isAuthenticated()")
  void recalculate(Long companyId, Long openAccountId);
}
