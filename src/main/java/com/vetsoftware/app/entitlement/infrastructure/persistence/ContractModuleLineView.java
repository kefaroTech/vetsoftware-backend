package com.vetsoftware.app.entitlement.infrastructure.persistence;

import java.time.LocalDate;

/**
 * Una linea del contrato ya proyectada sobre el submodulo que abre. El puente
 * entre vender y funcionar sale resuelto de la consulta, no de un bucle en
 * Java: un articulo puede abrir varios submodulos y resolverlo fila a fila
 * seria un N+1 en el camino mas caliente del sistema.
 *
 * <p>
 * <strong>Las dos columnas booleanas se proyectan como {@link Byte} y no como
 * {@code Boolean}, y no se puede cambiar</strong> (incidencia #472). Esto es
 * una proyeccion de {@code @Query(nativeQuery = true)}: el {@code ResultSet} lo
 * lee Spring Data directamente, sin pasar por el
 * {@code preferred_boolean_jdbc_type: TINYINT} que Hibernate aplica a los
 * atributos de una entidad gestionada. MySQL no tiene tipo booleano, asi que
 * Connector/J entrega una columna {@code TINYINT} como {@code java.lang.Byte},
 * y el {@code ProjectingMethodInterceptor} de Spring Data no encuentra ningun
 * converter {@code Byte -> Boolean}: en vez de convertir intenta tratar el
 * getter como una proyeccion anidada y revienta con
 * {@code UnsupportedOperationException: Cannot project java.lang.Byte to
 * java.lang.Boolean}. Eso tumbaba el alta de empresa entera --{@code POST
 * /register} devolvia 500-- en cuanto la consulta devolvia una sola fila.
 *
 * <p>
 * La salida <em>no</em> es declarar las columnas {@code TINYINT(1)} para que
 * Connector/J las devuelva como {@code Boolean}: el CLAUDE.md lo prohibe porque
 * el driver reporta {@code TINYINT(1)} como {@code BIT} y revienta el arranque
 * con {@code ddl-auto: validate}. Tampoco un {@code CAST} en el SQL, porque
 * MySQL no puede devolver un booleano por mucho que se le pida. Se proyecta el
 * numero y se compara contra cero en Java, que es exactamente lo que ya hace
 * {@code CompanyEntitlementJpaRepository} desde {@code
 * PROYECCION_SIN_LITERAL_BOOLEANO} (#196).
 */
public interface ContractModuleLineView {

    Long getSubscriptionItemId();

    Long getSubModuleId();

    String getSubModuleCode();

    String getSubModuleName();

    /** {@code sub_modules.read_only_capable} en crudo: 0 o 1, nunca un booleano. */
    Byte getReadOnlyCapable();

    LocalDate getEffectiveFrom();

    LocalDate getEffectiveTo();

    /** {@code catalog_items.is_core} en crudo: 0 o 1, nunca un booleano. */
    Byte getCore();
}
