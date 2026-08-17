package com.vetsoftware.app.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Las reglas del {@code CLAUDE.md}, ejecutables.
 *
 * <p>
 * Hay dos clases de regla aquí. Las que el código ya cumple van tal cual:
 * cualquier violación rompe el build. Las que encontraron deuda preexistente
 * van envueltas en {@link FreezingArchRule}, que tolera lo que está registrado
 * en {@code config/archunit/violation-store} y falla ante cualquier violación
 * nueva. Congelar no es perdonar: el store se versiona, se ve en el diff y solo
 * puede encoger.
 *
 * <p>
 * Para bajar deuda basta con arreglar el código y volver a correr el test:
 * ArchUnit quita del store las violaciones resueltas. Cuando una regla llegue a
 * cero, se le quita el {@code FreezingArchRule.freeze(...)} y pasa a ser dura.
 */
@AnalyzeClasses(packages = "com.vetsoftware.app", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    // ── Reglas duras: el código ya las cumple ────────────────────────────────

    @ArchTest
    static final ArchRule DOMINIO_SIN_FRAMEWORK = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..",
                    "jakarta.validation..", "com.fasterxml.jackson..", "tools.jackson..")
            .because("el dominio debe poder testearse sin contexto y sin BD");

    // ignoreDependency(origen, destino): lo que se exceptua es depender DE shared,
    // no que shared dependa de otros. RF-11 traia los argumentos al reves.
    @ArchTest
    static final ArchRule SIN_CRUCE_DE_DOMINIOS = slices()
            .matching("com.vetsoftware.app.(*)..domain..").should().notDependOnEachOther()
            .ignoreDependency(alwaysTrue(), resideInAPackage("..shared.."))
            .because("cross-feature se resuelve con companion VO (XxxRef), no importando");

    /**
     * El cierre de BE-03: sin esta regla, un puerto nuevo sin gate queda alcanzable
     * por cualquier JWT válido y nada lo detecta. La única excepción admitida es
     * {@link NoAuthorizationRequired}, que obliga a escribir el motivo.
     */
    @ArchTest
    static final ArchRule PUERTOS_AUTORIZADOS = methods().that().areDeclaredInClassesThat()
            .resideInAPackage("..application.port.in..").and().areDeclaredInClassesThat()
            .areInterfaces().and().areDeclaredInClassesThat()
            .areNotAnnotatedWith(NoAuthorizationRequired.class).should()
            .beAnnotatedWith(PreAuthorize.class)
            .because("la autorizacion vive en el puerto; un puerto nuevo sin gate queda abierto");

    /**
     * BE-08, ya sin deuda. Comprueba las dos formas en que un puerto recibe la
     * empresa —dentro del command y como parámetro suelto— y ademas que la
     * referencia SpEL apunte a un parámetro real: un {@code #nombre} que no existe
     * se resuelve a null y deja el gate siempre en false.
     */
    @ArchTest
    static final ArchRule TENANT_DEFENSA_EN_PROFUNDIDAD = methods().that()
            .areDeclaredInClassesThat().resideInAPackage("..application.port.in..").and()
            .areDeclaredInClassesThat().areInterfaces().and().areDeclaredInClassesThat()
            .areNotAnnotatedWith(NoAuthorizationRequired.class)
            .should(VetSoftwareConditions.validarElTenantCuandoRecibeCompanyId())
            .because("un companyId que no viene del principal es una fuga entre empresas");

    /**
     * El cierre de BE-29, y la otra mitad de la regla anterior. Aquella cubre los
     * puertos que <em>reciben</em> un {@code companyId}; esta, los que no reciben
     * ninguno y por eso pasaban sin que nadie mirara lo que servían por debajo.
     *
     * <p>
     * La fuga concreta —{@code GET /medicaments} devolviendo el catálogo privado de
     * todas las empresas a cualquiera con {@code prescription.read}— se tapó a mano
     * al encontrarla de casualidad. Esta regla es lo que la habría encontrado sola,
     * y lo que impide que la siguiente entre por el mismo sitio.
     */
    @ArchTest
    static final ArchRule LISTADOS_SIN_EMPRESA_SOLO_SYSTEM = classes().that()
            .resideInAPackage("..application.usecase..")
            .should(VetSoftwareConditions.cerrarASystemLosListadosSinEmpresa())
            .because("un listado que no filtra por empresa devuelve filas de todas:"
                    + " solo puede servirlo un principal cross-tenant");

    /**
     * El cierre de BE-21, y la regla sin la cual el hallazgo vuelve a crecer.
     *
     * <p>
     * «Una lista con su total y su número de página» es infraestructura de
     * colección, no semántica de negocio: por eso el vertical slicing —que sí
     * justifica duplicar tipos de dominio— no la cubre. Cuando cada feature declaró
     * la suya, la cuenta pasó de 12 a 36 en una semana, y no por descuido: paginar
     * una feature nueva copiando el {@code PageResult} del vecino era literalmente
     * lo que pedía la regla de no compartir DTOs.
     *
     * <p>
     * Quedan dos, uno a cada lado de la frontera y ambos únicos:
     * {@code shared.pagination.PageResult} para dentro e
     * {@code infrastructure.web.PageResponse} para el JSON. Esta regla es lo que
     * impide que entre el tercero con la próxima feature paginada.
     */
    @ArchTest
    static final ArchRule PAGINACION_CON_UN_SOLO_CONTRATO = classes()
            .that(VetSoftwareConditions.declaranElConceptoDePagina()).should()
            .resideInAnyPackage("com.vetsoftware.app.shared.pagination",
                    "com.vetsoftware.app.infrastructure.web")
            .because("la pagina es un tipo sin semantica de negocio: uno por lado"
                    + " de la frontera, no uno por feature");

    /**
     * La otra mitad de {@link #PAGINACION_CON_UN_SOLO_CONTRATO}: unificar el tipo
     * no sirve de nada si cada adaptador sigue traduciendo a mano lo que pide el
     * cliente. Ahí la copia no era solo ruido —tres adaptadores pasaban los
     * parámetros crudos a {@code PageRequest.of}, sin normalizar el índice ni topar
     * el tamaño, y un {@code ?pageSize=100000} deshacía el trabajo de paginar.
     */
    @ArchTest
    static final ArchRule PAGINA_ACOTADA_EN_UN_SOLO_SITIO = noClasses().that()
            .resideOutsideOfPackage("com.vetsoftware.app.shared.pagination").should()
            .callMethodWhere(VetSoftwareConditions.esUnPageRequestSinAcotar())
            .because("el tope de filas por pagina se decide en Pages.request(), no por adaptador");

    /**
     * Lo que mantiene a Spring Data fuera de {@code application}. El kernel tiene
     * dos piezas y solo una conoce el framework: {@code PageResult} lo cruza todo,
     * {@code Pages} se queda en el adaptador. Sin esta regla, un caso de uso que
     * llame a {@code Pages.request(...)} arrastra {@code PageRequest} a la capa que
     * el CLAUDE.md exige poder probar sin contexto — y no lo detectaría ninguna de
     * las otras dos, porque la llamada a {@code PageRequest.of} ocurre dentro del
     * kernel.
     */
    @ArchTest
    static final ArchRule PUENTE_DE_PAGINACION_SOLO_EN_PERSISTENCIA = noClasses().that()
            .resideOutsideOfPackages("..infrastructure.persistence..",
                    "com.vetsoftware.app.shared.pagination")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("com.vetsoftware.app.shared.pagination.Pages")
            .because("Pages habla Spring Data: application se queda con PageResult");

    /**
     * Dura: ya no queda ninguna. La búsqueda se detiene en los saltos
     * {@code @Async}, así que el envío de correos —asíncrono a propósito— no cuenta
     * como HTTP dentro de la transacción.
     */
    @ArchTest
    static final ArchRule SIN_IO_EXTERNO_EN_TRANSACCION = noMethods().that()
            .areAnnotatedWith(Transactional.class)
            .should(VetSoftwareConditions.alcanzarUnClienteHttp(RestClient.class))
            .because("una llamada HTTP retiene la conexion y los locks hasta el commit");

    // ── Reglas congeladas: deuda preexistente, cero violaciones nuevas ───────

    @ArchTest
    static final ArchRule APPLICATION_NO_CONOCE_INFRASTRUCTURE = FreezingArchRule
            .freeze(noClasses().that().resideInAPackage("..application..").should()
                    .dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("infrastructure -> application -> domain, nunca al reves"));

    @ArchTest
    static final ArchRule REPOS_CON_ENTITYGRAPH = FreezingArchRule.freeze(classes().that()
            .areAssignableTo(JpaRepository.class).and().haveSimpleNameEndingWith("JpaRepository")
            .should(VetSoftwareConditions.declararEntityGraphEnLosFinders())
            .because("la regla del CLAUDE.md sobre N+1 debe ser verificable"));

    private static final DescribedPredicate<JavaMethodCall> FIND_ALL_SIN_ARGS = DescribedPredicate
            .describe("es findAll() sin argumentos",
                    call -> "findAll".equals(call.getTarget().getName())
                            && call.getTarget().getRawParameterTypes().isEmpty());

    @ArchTest
    static final ArchRule SIN_FINDALL_SIN_TENANT = FreezingArchRule.freeze(noClasses().that()
            .resideInAPackage("..application.usecase..").should().callMethodWhere(FIND_ALL_SIN_ARGS)
            .because("un findAll() sin companyId es una fuga entre empresas esperando a ocurrir"));

    /**
     * El cierre de BE-18, y el complemento exacto de
     * {@link #SIN_IO_EXTERNO_EN_TRANSACCION}: aquella protege la transacción del
     * I/O, esta protege al cliente de la transacción. Un efecto {@code @Async}
     * disparado antes del commit se entrega igual aunque la transacción revierta
     * después, y no hay forma de retirarlo: el correo ya está en la bandeja.
     *
     * <p>
     * Congelada porque el correo transaccional del proyecto nació así: la cita no
     * era el único sitio, solo el que se miró. El store registra las otras cuatro
     * notificaciones que hoy salen sin esperar al commit —invitación de empleado,
     * reenvío de invitación, restablecer contraseña y recuperar código—, todas con
     * la misma consecuencia: el correo llega y la operación no ocurrió.
     *
     * <p>
     * La forma de resolver una violación no es tocar la regla, sino diferir el
     * efecto: resolver los datos dentro de la transacción y registrar el envío en
     * {@code afterCommit}. {@code CreateAppointmentService.sendAfterCommit} y
     * {@code EmitElectronicDocumentOnCloseService} son las dos referencias.
     */
    @ArchTest
    static final ArchRule EFECTOS_ASINCRONOS_DESPUES_DEL_COMMIT = FreezingArchRule
            .freeze(noMethods().that().areAnnotatedWith(Transactional.class).or()
                    .areDeclaredInClassesThat().areAnnotatedWith(Transactional.class)
                    .should(VetSoftwareConditions.alcanzarUnEfectoAsincrono())
                    .because("lo que cruza de hilo antes del commit no vuelve si hay rollback"));
}
