package com.vetsoftware.app.subscription.application.port.out;

/**
 * Valida el usuario de plataforma que firma un otrosi. {@code system_users} es
 * una tabla global, sin empresa por la que acotar.
 */
public interface SystemUserValidationPort {
    void validateExists(Long systemUserId);
}
