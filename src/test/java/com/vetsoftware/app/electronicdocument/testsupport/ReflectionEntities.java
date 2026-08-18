package com.vetsoftware.app.electronicdocument.testsupport;

/**
 * Construye entidades JPA de OTRAS features (constructor protegido, sin builder
 * publico) para los adaptadores de query-port de esta feature que las consultan
 * por cruce permitido de vertical slicing (ver CLAUDE.md, seccion
 * "Cross-feature references"). No sustituye al object mother de esta feature:
 * solo evita construir dobles/mocks de una entidad de persistencia ajena.
 */
public final class ReflectionEntities {

    private ReflectionEntities() {
    }

    public static <T> T newInstance(Class<T> type) throws ReflectiveOperationException {
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
