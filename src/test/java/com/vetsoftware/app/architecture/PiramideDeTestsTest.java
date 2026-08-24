package com.vetsoftware.app.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

/**
 * BE-10, ejecutable: <em>todo adaptador tiene su rodaja</em> — y, desde que hay
 * un {@code @AnalyzeClasses} que ve {@code src/test}, también lo que ese árbol
 * le puede hacer a producción ({@code DOBLE_DE_TEST_NO_ESCANEABLE}).
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
 * {@code src/test} fuera del universo importado. Las reglas que necesitan ver
 * los dos árboles a la vez —«cada adaptador con su rodaja», que busca la rodaja
 * en {@code src/test}, y «ningún doble de test escaneable», que mira
 * {@code src/test} entero— necesitan su propio {@code @AnalyzeClasses} sin esa
 * opción. Se resolvió así, y no con un test que recorriera ficheros a mano,
 * porque el congelado ya existe y es de ArchUnit: un recorrido de directorios
 * habría necesitado su propio fichero de línea base, su propio formato y su
 * propio código para quitar de él lo ya resuelto — reinventar
 * {@link FreezingArchRule} con menos garantías.
 *
 * <p>
 * <strong>Las dos reglas de BE-10 nacieron congeladas y ya no lo
 * están.</strong> Empezaron con 74 adaptadores y 78 controllers sin rodaja: en
 * duro habrían dejado el build en rojo permanente, que es la forma habitual de
 * acabar borrando una regla. El congelado hizo lo que pedía el defecto — el
 * número vivía en {@code config/archunit/violation-store}, se veía en el diff y
 * solo podía bajar. El 2026-08-24 llegó a cero y se les quitó el
 * {@code FreezingArchRule.freeze(...)}, igual que se hizo con
 * {@code SIN_IO_EXTERNO_EN_TRANSACCION}. Sus dos ficheros de línea base se
 * borraron: un almacén vacío de una regla que ya no congela no es una foto de
 * deuda, es un sitio donde volver a esconderla.
 *
 * <p>
 * <strong>Por qué importa que sean duras y no congeladas en cero.</strong> No
 * es lo mismo. Congelada en cero, añadir un adaptador sin rodaja actualiza el
 * almacén y el build sigue verde si nadie mira el diff; dura, rompe el build el
 * mismo día que entra. Ese es el trinquete: lo que costó 117 pruebas contra
 * MySQL real no se puede perder por un fichero que alguien regenera sin leerlo.
 *
 * <p>
 * <strong>Cuidado si tocas la descripción.</strong> El almacén indexa por el
 * texto completo de la regla. Mientras quede alguna congelada en este proyecto,
 * cambiar un predicado o un {@code because} huérfana su foto y hace reaparecer
 * la deuda entera.
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
    static final ArchRule ADAPTADOR_JPA_CON_RODAJA = classes().that()
            .resideInAPackage("..infrastructure.persistence").and()
            .haveSimpleNameStartingWith("Jpa").and().haveSimpleNameEndingWith("Repository")
            .and(VetSoftwareConditions.sonCodigoDeProduccion())
            .should(VetSoftwareConditions.tenerRodajaDePersistencia())
            .because("sin rodaja, el SQL del adaptador no lo ejecuta nadie hasta produccion");

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
    static final ArchRule CONTROLLER_CON_RODAJA = classes().that()
            .areAnnotatedWith(RestController.class)
            .and(VetSoftwareConditions.sonCodigoDeProduccion())
            .should(VetSoftwareConditions.tenerRodajaWeb())
            .because("el gate, la validacion y la forma del JSON solo se ven"
                    + " desde un @WebMvcTest");

    /**
     * Ningún doble de test puede ser candidato al escaneo de producción: todo lo
     * que el árbol de test declara como registrable va marcado como de test.
     *
     * <p>
     * <strong>El defecto.</strong> Un test declaró su cableado como
     * {@code @Configuration} simple en vez de {@code @TestConfiguration}. Vive
     * dentro de la raíz del {@code @ComponentScan} y {@code target/test-classes}
     * está en el classpath de failsafe, así que se volvió candidata de escaneo: su
     * doble de {@code CreateAppointmentUseCase} se registraba en <em>todos</em> los
     * contextos y competía con el service real, de forma que
     * {@code AppointmentController} encontraba dos beans y <b>ningún
     * {@code @SpringBootTest} del repositorio arrancaba</b>. Noventa segundos de
     * build para morir con un {@code NoUniqueBeanDefinitionException} que no señala
     * a la causa.
     *
     * <p>
     * <strong>Y el mecanismo es más sutil de lo que parece.</strong>
     * {@code TestTypeExcludeFilter} sí descarta las clases anidadas dentro de una
     * clase de test — pero la culpable tenía <em>todos</em> sus {@code @Test}
     * dentro de {@code @Nested}, así que no se la reconoce como clase de test y su
     * anidada quedó expuesta. Se verificó por contraste:
     * {@code AsyncObservedIntegrationTest} y {@code GlobalExceptionHandlerTest}
     * tienen un {@code @Test} de primer nivel y por eso nunca se escanearon.
     * Traducido: la diferencia entre bomba y bomba desactivada es si la clase
     * externa tiene un {@code @Test} suelto — una condición invisible en revisión,
     * que se arma sola el día que alguien refactoriza el último test a un
     * {@code @Nested}. Esta regla la convierte en una invariante sintáctica.
     *
     * <p>
     * <strong>Por qué {@code @Component} en el predicado y {@code @TestComponent}
     * en la condición.</strong> El predicado es ancho a propósito:
     * {@code @Configuration} era el caso que estalló, pero {@code @RestController},
     * {@code @Service}, {@code @Repository} y {@code @Component} son igual de
     * escaneables, y todos están meta-anotados con {@code @Component}. La condición
     * es {@code @TestComponent} porque es exactamente la señal que lee el
     * {@code TypeExcludeFilter} de Boot, y es lo que comparten las dos formas
     * correctas: {@code @TestConfiguration} está meta-anotada con él, así que una
     * configuración de test bien escrita pasa sin mencionarla, y un doble que no
     * sea configuración —un controller de juguete— se marca con
     * {@code @TestComponent} a secas. Exigir {@code @TestConfiguration} habría
     * dejado ese segundo caso sin salida legítima.
     *
     * <p>
     * <strong>Meta-anotado y no anotado</strong>, en los dos lados: así la regla se
     * lee como «toda clase registrable del árbol de test está marcada como de
     * test», y no como un {@code notExist()} de las malas con peor mensaje de
     * fallo.
     *
     * <p>
     * <strong>Nace dura, sin {@code freeze(...)}.</strong> Tenía una sola violación
     * —{@code GlobalExceptionHandlerTest.BoomController}, hoy ya anotado— y una
     * regla de una línea de deuda no se congela: se arregla.
     *
     * @see VetSoftwareConditions#vienenDelArbolDeTest()
     */
    @ArchTest
    static final ArchRule DOBLE_DE_TEST_NO_ESCANEABLE = classes()
            .that(VetSoftwareConditions.vienenDelArbolDeTest()).and()
            .areMetaAnnotatedWith(Component.class).should().beMetaAnnotatedWith(TestComponent.class)
            .because("una clase registrable del arbol de test sin @TestComponent es"
                    + " candidata al escaneo de produccion: se cuela en todos los"
                    + " contextos y compite con el bean real");
}
