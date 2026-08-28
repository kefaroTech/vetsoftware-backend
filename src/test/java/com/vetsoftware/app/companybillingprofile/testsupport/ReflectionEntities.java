package com.vetsoftware.app.companybillingprofile.testsupport;

/**
 * Construye entidades JPA de OTRAS features, que declaran su constructor sin
 * argumentos {@code protected} y no tienen builder publico.
 *
 * <p>
 * Lo necesita el test del mapper: {@code CityJpaEntity} vive en
 * {@code city.infrastructure.persistence} y el constructor protegido no es
 * accesible desde aqui. La alternativa —mockear la entidad— esta prohibida por
 * el CLAUDE.md, y con razon: un doble no valida sus propias invariantes.
 *
 * <p>
 * Es una copia del homonimo de {@code companytaxprofile} y eso es lo correcto:
 * el vertical slicing aplica igual en {@code src/test}, asi que un paquete de
 * fixtures compartido entre features seria la capa horizontal que el documento
 * prohibe.
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
