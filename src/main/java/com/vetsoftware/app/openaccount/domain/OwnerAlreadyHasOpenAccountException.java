package com.vetsoftware.app.openaccount.domain;

/**
 * Regla de negocio: un propietario solo puede tener UNA cuenta abierta a la vez.
 * Se lanza al intentar abrir una cuenta para un propietario que ya tiene una activa.
 */
public class OwnerAlreadyHasOpenAccountException extends RuntimeException {
    public OwnerAlreadyHasOpenAccountException(Long ownerId) {
        super("Owner already has an open account: " + ownerId);
    }
}
