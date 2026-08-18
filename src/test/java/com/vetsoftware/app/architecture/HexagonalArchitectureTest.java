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
     * La tercera pieza del trío de tenancy, y el cierre de BE-COV.
     * {@link #TENANT_DEFENSA_EN_PROFUNDIDAD} mira los puertos que <em>reciben</em>
     * un {@code companyId}; {@link #LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}, los que no
     * reciben ninguno y devuelven <em>varias</em> filas. Entre las dos quedaba el
     * hueco por el que pasó una campaña entera: las operaciones que no reciben
     * empresa y actúan sobre <em>una</em> fila, señalada por un id que el cliente
     * escribe en la URL.
     *
     * <p>
     * La auditoría de cobertura encontró ~65 de esas en 27 de 94 features
     * —{@code DELETE /employee-roles/{id}} revocando el rol del administrador de
     * otra empresa, {@code PATCH /employees/{id}/enable} devolviéndole el acceso a
     * quien otro tenant despidió, {@code GET /laboratory-test-files/{id}/download}
     * entregando el PDF de un resultado de laboratorio ajeno— y ArchUnit pasó 13/13
     * en verde mientras todas existían. Esa es la razón de la regla: la lección de
     * BE-29 fue que tapar la fuga sin tocar el mecanismo que la dejó pasar la trae
     * de vuelta.
     */
    @ArchTest
    static final ArchRule OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM = methods().that()
            .areDeclaredInClassesThat().resideInAPackage("..application.port.in..").and()
            .areDeclaredInClassesThat().areInterfaces().and().areDeclaredInClassesThat()
            .areNotAnnotatedWith(NoAuthorizationRequired.class)
            .should(VetSoftwareConditions.acotarPorEmpresaLasOperacionesPorId())
            .because("un id lo escribe el cliente en la URL: si la fila es de una empresa,"
                    + " el puerto tiene que recibir cual");

    /**
     * La misma familia de fugas vista desde el SQL, y su variante más grave. En un
     * {@code delete} o un {@code update} corriente hay una lectura previa que
     * valida la propiedad; en un {@code reactivate} no la hay —el servicio decide
     * si la fila existe mirando cuántas actualizó—, así que el {@code WHERE} es
     * toda la seguridad que hay.
     *
     * <p>
     * Un {@code UPDATE employee_roles SET enabled = true WHERE id = :id} reactivaba
     * una asignación de rol revocada a un empleado de otra empresa y le vaciaba la
     * caché de permisos: escalada de privilegios cross-tenant en cuatro líneas de
     * SQL, invisible para las otras dos reglas porque el puerto sí recibía su
     * {@code companyId} —y lo ignoraba.
     */
    @ArchTest
    static final ArchRule MUTACIONES_SQL_ACOTADAS_POR_EMPRESA = classes().that()
            .areAssignableTo(JpaRepository.class).and().haveSimpleNameEndingWith("JpaRepository")
            .should(VetSoftwareConditions.acotarPorEmpresaElSqlQueEscribe())
            .because("en un reactivate no hay lectura previa: el WHERE es la unica barrera");

    /**
     * La fuga que ninguna revisión humana ve, porque la anotación «se ve bien».
     * Doce {@code Update…UseCase} llevaban
     * {@code @authz.isMyCompany(#command.companyId)} y eran vulnerables igualmente:
     * esa anotación solo prueba que el atacante declara <em>su propia</em> empresa,
     * no de quién es la fila que el servicio está a punto de cargar. Con
     * {@code findById(command.id())} seguido de {@code entidad.update(…, company)},
     * el efecto no es un rechazo sino una <b>apropiación</b>: la fila de la empresa
     * B pasa a ser de A.
     *
     * <p>
     * No prohíbe {@code findById}: el camino SYSTEM legítimo es el ternario
     * {@code companyId == null ? findById(id) : findByIdAndCompanyId(id, companyId)},
     * que llama a las dos variantes. Lo que exige es que la clase llame
     * <em>también</em> a la acotada cuando el puerto la ofrece; la fuga es la clase
     * que solo conoce la variante ancha. Referencia: {@code UpdateSpaService}.
     *
     * <p>
     * <b>«La acotada» es una familia, no un nombre.</b> Los cuatro catálogos por
     * empresa declaran <em>dos</em> finders acotados con propósitos distintos:
     * {@code findByIdAndCompanyId} para leer (la fila propia o cualquiera de las
     * generales) y {@code findOwnedByIdAndCompanyId} para escribir (SOLO la propia,
     * porque editar una general la cambiaría para todos los tenants). Sus ocho
     * {@code Update…}/{@code Delete…} usan el segundo —el estrictamente más fuerte—
     * y la condición los marcaba por no llamar al primero: pedía cambiar código
     * correcto por código menos seguro. Ahora empareja por la cláusula {@code By…}
     * y el tipo de retorno, así que vale cualquier hermana que cargue lo mismo por
     * el mismo criterio con la empresa encima.
     *
     * <p>
     * <b>También está exento el servicio sin autorización de empleado.</b> Un
     * puerto anotado {@code @NoAuthorizationRequired} declara por escrito que su
     * autorización no es un JWT —el token de un solo uso del flujo público de
     * verificación, la firma HMAC del webhook del proveedor—, así que no hay
     * principal del que sacar una empresa; en el webhook la empresa es incluso una
     * <em>salida</em> de la búsqueda.
     * {@link #OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM} ya lo trataba como
     * exención en su {@code .that()}: no mirarlo aquí era una asimetría entre dos
     * reglas de la misma familia, no una decisión.
     */
    @ArchTest
    static final ArchRule CARGA_POR_ID_ACOTADA_POR_EMPRESA = classes().that()
            .resideInAPackage("..application.usecase..")
            .should(VetSoftwareConditions.cargarPorIdAcotandoLaEmpresa())
            .because("cargar por id sin acotar y reescribir la empresa no rechaza: se apropia"
                    + " de la fila ajena");

    /**
     * La cuarta forma del defecto, y la que sobrevive a las otras tres. Con la
     * carga propia ya acotada, un {@code UpdateSurgeryService} no puede apropiarse
     * de una cirugía ajena; lo que sí puede es <b>reapuntar la suya a una entidad
     * de otro tenant</b>, porque resuelve el animal con
     * {@code animalQueryPort.findById(command.animalId())} y ese puerto no filtra
     * nada: una cirugía de mi empresa colgada del animal de la vecina, con su
     * historia clínica contaminada. Afecta a {@code laboratorytest},
     * {@code surgery}, {@code diagnosticimaging} y {@code daycare}; {@code spa},
     * {@code prescription} y {@code consultation} son el modelo.
     *
     * <p>
     * Es regla aparte y no una ampliación de
     * {@link #CARGA_POR_ID_ACOTADA_POR_EMPRESA} porque aquella ya mira estos
     * puertos —su filtro es el paquete, no el nombre del tipo— y no le sirve de
     * nada: el problema es que <b>no declaran</b> ninguna variante acotada que
     * exigirles llamar. «Declárala» y «llámala» son dos afirmaciones distintas y el
     * mensaje de fallo tiene que decir cuál es. Las dos quedan disjuntas por
     * construcción y se comprobó que no comparten ni un punto.
     *
     * <p>
     * <b>Solo mira servicios que ya tienen el {@code companyId} en la mano</b> —los
     * que llaman a alguna variante acotada en otra parte— y solo las llamadas que
     * <em>resuelven la referencia</em> ({@code find…} que devuelve un
     * {@code XxxRef}), no los predicados del mismo puerto ({@code isOpen},
     * {@code lockForUpdate}, {@code outstandingAmount}). Sin esos dos cortes la
     * condición marcaba 89 puntos en vez de 38, casi todos {@code Create…Service}:
     * ahí el defecto es el mismo pero la regla no puede distinguir un id que llega
     * del cliente de uno que llega del principal.
     *
     * <p>
     * <b>El falso positivo que se dejó visible, y cómo se cerró sin enumerar
     * puertos.</b> Siete servicios resolvían {@code EmployeeQueryPort.findById}
     * para guardar el <em>empleado autenticado</em> como {@code createdBy} /
     * {@code processedBy} / {@code suspendedBy}. Ahí el id viene del principal y no
     * hay nada que acotar, pero la primera versión de la regla los dejó dentro a
     * propósito: excluirlos exigía enumerar un puerto por su nombre —lo que este
     * fichero evita en todas las demás reglas— y eso taparía también el día que un
     * {@code employeeId} llegue de verdad en el request.
     *
     * <p>
     * La señal que los separa no es el nombre del puerto sino la <b>ausencia del
     * nombre de recurso</b>: si la referencia es a {@code EmployeeJpaEntity} y
     * ningún command ni parámetro del servicio declara un {@code employeeId}, el
     * servicio no tiene por dónde recibir «sobre qué empleado actúo». La condición
     * lo comprueba en {@code nombraLaReferenciaComoAutor}, y el día que alguien
     * añada ese campo al command la regla vuelve a marcarlo — la preocupación
     * original queda intacta. Discrimina de verdad: en
     * {@code CreateOpenAccountService} exime el {@code createdById} y deja rojo el
     * {@code OwnerQueryPort}, porque su command sí declara {@code ownerId}.
     */
    @ArchTest
    static final ArchRule REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA = classes().that()
            .resideInAPackage("..application.usecase..")
            .should(VetSoftwareConditions.acotarPorEmpresaLasReferenciasCrossFeature())
            .because("una referencia sin acotar no se apropia de la fila ajena: cuelga la propia"
                    + " de un padre de otro tenant");

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
