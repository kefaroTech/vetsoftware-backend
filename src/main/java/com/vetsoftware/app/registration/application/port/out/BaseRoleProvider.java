package com.vetsoftware.app.registration.application.port.out;

import java.util.List;

/**
 * Provee los roles base (plantillas globales) para instanciarlos en la empresa
 * al registrarse. Se instancian TODOS los base roles habilitados; el flag
 * {@code mandatory} indica cuáles debe asumir automáticamente el dueño.
 */
public interface BaseRoleProvider {
    List<BaseRoleData> findAll();

    record BaseRoleData(Long id, String name, String code, boolean mandatory) {
    }
}
