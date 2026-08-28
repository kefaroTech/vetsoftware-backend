package com.vetsoftware.app.limitdimension.domain;

/**
 * El submódulo del que cuelga un eje, visto desde esta feature. Companion VO:
 * el dominio de {@code submodule} no se importa (vertical slicing), y lo que
 * hace falta aquí es el identificador y el nombre para poder explicar de dónde
 * sale el contador.
 *
 * <p>
 * «Mascotas» cuelga de Historia clínica: si la clínica no tiene ese módulo, ese
 * contador ni siquiera existe para ella.
 */
public record SubModuleRef(Long id, String code, String name) {

    public SubModuleRef {
        if (id == null)
            throw new IllegalArgumentException("sub module id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("sub module code is required");
    }
}
