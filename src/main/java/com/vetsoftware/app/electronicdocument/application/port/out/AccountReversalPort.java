package com.vetsoftware.app.electronicdocument.application.port.out;

/**
 * Puerto de salida hacia la cartera (open account): marca una cuenta como
 * reversada cuando la nota credito que la corrige es VALIDADA por la DIAN.
 * Idempotente. El adapter vive en infrastructure y es el unico que conoce la
 * feature openaccount (cruce permitido en persistence).
 */
public interface AccountReversalPort {
    void markReversed(Long openAccountId);
}
