package com.vetsoftware.app.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.springframework.web.bind.annotation.RestController;

/**
 * BE-10, ejecutable: <em>todo adaptador tiene su rodaja</em>.
 *
 * <p>
 * El hallazgo de auditoría no era «faltan tests», era que los dos adaptadores
 * —el que traduce a SQL y el que traduce a HTTP— no tenían ninguna prueba que
 * los ejercitara <em>como adaptadores</em>: el mapper se probaba en aislamiento
 * y el caso de uso con mocks, así que un {@code Sort} sin desempate, un
 * {@code @PreAuthorize} mal escrito o un JSON que dejó de cuadrar no los
 * detectaba nadie hasta producción. La primera mitad del defecto se cerró
 * escribiendo 26 rodajas. Esta clase es la segunda mitad, y es la que hace que
 * el cierre valga: sin ella el indicador se calcula a mano con {@code grep} y
 * nada impide que mañana vuelva a cero. BE-29 ya enseñó lo que pasa cuando se
 * tapa la fuga y se deja intacto el mecanismo que la dejó pasar.
 *
 * <p>
 * <strong>Por qué una clase aparte y no una regla más en
 * {@link HexagonalArchitectureTest}.</strong> Aquella declara
 * {@code ImportOption.DoNotIncludeTests}, que es lo correcto para lo que
 * comprueba —las reglas de arquitectura miran producción— pero deja
 * {@code src/test} fuera del universo importado. «Cada adaptador con su rodaja»
 * es la única regla del proyecto que necesita ver los dos árboles a la vez, así
 * que necesita su propio {@code @AnalyzeClasses} sin esa opción. Se resolvió
 * así, y no con un test que recorriera ficheros a mano, porque el congelado ya
 * existe y es de ArchUnit: un recorrido de directorios habría necesitado su
 * propio fichero de línea base, su propio formato y su propio código para
 * quitar de él lo ya resuelto — reinventar {@link FreezingArchRule} con menos
 * garantías.
 *
 * <p>
 * <strong>Las dos reglas nacen congeladas.</strong> Quedan 74 adaptadores y 78
 * controllers sin rodaja; en duro dejarían el build en rojo permanente, que es
 * la forma habitual de acabar borrando una regla. Congeladas hacen exactamente
 * lo que pedía el defecto: el número está en
 * {@code config/archunit/violation-store}, se ve en el diff y <strong>solo
 * puede bajar</strong>. Un adaptador o un controller nuevo sin su rodaja rompe
 * el build el mismo día que entra.
 *
 * <p>
 * <strong>Cuando el store llegue a cero</strong> se les quita el
 * {@code FreezingArchRule.freeze(...)} y pasan a duras, igual que se hizo con
 * {@code SIN_IO_EXTERNO_EN_TRANSACCION}. Y mientras tanto, cuidado con la
 * descripción: el store indexa por el texto completo de la regla, así que
 * cambiar un predicado o el {@code because} huérfana la foto y la deuda vuelve
 * a aparecer entera.
 *
 * @see VetSoftwareConditions#tenerRodajaDePersistencia()
 * @see VetSoftwareConditions#tenerRodajaWeb()
 * @see VetSoftwareConditions#sonCodigoDeProduccion()
 */
@AnalyzeClasses(packages = "com.vetsoftware.app")
class PiramideDeTestsTest {

    /**
     * Un adaptador JPA sin rodaja es SQL que nadie ejecuta hasta producción: el
     * test del mapper no ve la consulta, y el del caso de uso mockea el puerto
     * entero. Lo que solo cubre un {@code @DataJpaTest} contra MySQL real es lo
     * caro de descubrir tarde — el {@code Sort} sin desempate que repite filas
     * entre páginas, el {@code findAll} que se olvidó del {@code companyId}, el
     * {@code @EntityGraph} que no evita el N+1 que creía evitar.
     *
     * <p>
     * El alcance son los adaptadores con el naming del CLAUDE.md ({@code Jpa} +
     * entidad + {@code Repository}), los 91 que implementan un puerto de salida de
     * escritura. Los {@code JpaXxxQueryPort} y los adaptadores sueltos quedan fuera
     * a propósito: ver {@link VetSoftwareConditions#tenerRodajaDePersistencia()}.
     */
    @ArchTest
    static final ArchRule ADAPTADOR_JPA_CON_RODAJA = FreezingArchRule.freeze(classes().that()
            .resideInAPackage("..infrastructure.persistence").and()
            .haveSimpleNameStartingWith("Jpa").and().haveSimpleNameEndingWith("Repository")
            .and(VetSoftwareConditions.sonCodigoDeProduccion())
            .should(VetSoftwareConditions.tenerRodajaDePersistencia())
            .because("sin rodaja, el SQL del adaptador no lo ejecuta nadie hasta produccion"));

    /**
     * Un controller sin rodaja no tiene comprobado nada de lo que solo vive en él:
     * ni el {@code @PreAuthorize} que lo protege, ni la validación del
     * {@code @Valid}, ni el código de estado, ni la forma del JSON que el contrato
     * OpenAPI promete a los dos fronts. El test del caso de uso no llega ahí — el
     * caso de uso ya recibe el command construido.
     *
     * <p>
     * El predicado es la anotación, no el nombre: un {@code @RestController} que se
     * llame de otra forma cuenta igual, y el {@code GlobalExceptionHandler}
     * —{@code @RestControllerAdvice}— no cuenta, porque esa anotación no está
     * meta-anotada con {@code @RestController}.
     *
     * <p>
     * El filtro de {@link VetSoftwareConditions#sonCodigoDeProduccion()} no es
     * decorativo: sin él la regla exigía una rodaja para
     * {@code GlobalExceptionHandlerTest.BoomController}, el controller de juguete
     * del test del handler. Importar {@code src/test} para encontrar las rodajas
     * mete también sus fixtures en el universo analizado.
     */
    @ArchTest
    static final ArchRule CONTROLLER_CON_RODAJA = FreezingArchRule
            .freeze(classes().that().areAnnotatedWith(RestController.class)
                    .and(VetSoftwareConditions.sonCodigoDeProduccion())
                    .should(VetSoftwareConditions.tenerRodajaWeb())
                    .because("el gate, la validacion y la forma del JSON solo se ven"
                            + " desde un @WebMvcTest"));
}
