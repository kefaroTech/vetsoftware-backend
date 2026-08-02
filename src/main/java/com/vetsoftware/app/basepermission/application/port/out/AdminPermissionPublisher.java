package com.vetsoftware.app.basepermission.application.port.out;

/**
 * Propaga el catálogo ADMIN actualizado a los roles empresariales existentes.
 */
public interface AdminPermissionPublisher {
    void publish();
}
