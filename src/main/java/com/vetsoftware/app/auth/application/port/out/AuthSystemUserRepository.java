package com.vetsoftware.app.auth.application.port.out;

import java.util.Optional;

/** Estado mínimo de autenticación de un usuario administrador del sistema. */
public interface AuthSystemUserRepository {

  Optional<AuthSystemUser> findActiveById(Long systemUserId);

  /** Rota la versión bajo bloqueo de fila para serializar logins concurrentes. */
  Optional<AuthSystemUser> rotateAuthVersion(Long systemUserId);

  /** Invalida de inmediato el access token actualmente emitido. */
  void bumpAuthVersion(Long systemUserId);

  record AuthSystemUser(Long id, Long authVersion) {}
}
