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

    /**
     * {@code subscription_items.charge_mode}: {@code TRIAL}, {@code PAID},
     * {@code FREE_LIMITED} o {@code EXPIRED_READ_ONLY}.
     *
     * <p>
     * <strong>Es la columna que decide si esta linea cobra y si caduca</strong>, y
     * viaja por linea a proposito: el estado del contrato dejo de significar "a
     * este cliente no se le cobra", porque un mismo contrato lleva a la vez lineas
     * en prueba y lineas de pago obligatorio (R-TRIAL-13).
     */
    String getChargeMode();

    /**
     * {@code subscription_items.trial_end_date}: el ultimo dia de prueba de
     * <strong>esta</strong> linea, inclusive. Vacio si la linea no esta en prueba.
     *
     * <p>
     * Cada linea vence por su cuenta (R-TRIAL-15). Barrer por el estado del
     * contrato en vez de por esta fecha es lo que hace que un solo dia de mora mate
     * la prueba de los tres modulos a la vez y para siempre.
     */
    LocalDate getTrialEndDate();

    /**
     * {@code company_trial_grants.policy_trial_outcome}: el desenlace
     * <strong>congelado el dia que se concedio</strong> la prueba, no la politica
     * viva del catalogo (R-TRIAL-28). Es lo que decide si la fila sucesora nace
     * gratuita con techo, de pago o en solo lectura.
     */
    String getTrialOutcome();

    /**
     * {@code sub_modules.degradation_immune} en crudo: 0 o 1, nunca un booleano. Un
     * submodulo inmune no se degrada jamas (R-ENT-05).
     */
    Byte getDegradationImmune();
}
