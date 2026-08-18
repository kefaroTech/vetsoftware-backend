package com.vetsoftware.app.auth.testsupport;

/**
 * Construye entidades JPA de OTRAS features (constructor protegido, sin builder
 * publico) para los adaptadores de esta feature que las consultan por cruce
 * permitido de vertical slicing (ver CLAUDE.md, seccion "Cross-feature
 * references"). No sustituye a {@link AuthMother}: solo evita construir
 * dobles/mocks de una entidad de persistencia ajena — dobles que, sobre
 * entidades con bytecode de Hibernate ya realzado en compilacion, resultan
 * ademas inestables con el mock maker inline de Mockito.
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
